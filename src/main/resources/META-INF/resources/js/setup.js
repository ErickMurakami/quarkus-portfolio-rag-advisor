'use strict';

// Draft and job data stay in memory. No PDF, model text or job data enters storage.
if (byId('setup-form')) {
  appState.setup = { fullName: '', githubUsername: '', skills: [], mode: 'manual', developerId: null, job: null, reviewed: false };
  const draft = appState.setup;
  const categories = byId('setup-root').dataset.categories.split(',');
  const jobStatuses = ['PENDING', 'RUNNING', 'WAITING_RATE_LIMIT', 'COMPLETED', 'FAILED'];
  let pollingTimer;
  let importVersion = 0;
  let importController;
  let saving = false;

  function username() {
    const value = byId('setup-username').value.trim();
    if (!/^[A-Za-z0-9](?:[A-Za-z0-9-]{0,28}[A-Za-z0-9])?$/.test(value)) throw new Error('Enter a valid GitHub username (up to 30 characters).');
    return value;
  }
  function addSkill(skill = { name: '', category: categories[0] }) {
    if (draft.skills.length >= 100) throw new Error('A profile supports up to 100 skills.');
    const value = { name: skill.name ?? '', category: skill.category ?? categories[0] };
    draft.skills.push(value);
    const row = element('div', null, 'skill-editor');
    const name = element('input'); name.value = value.name; name.maxLength = 40;
    name.setAttribute('aria-label', translated('Skill name', appState.language)); name.dataset.originalLabel = 'Skill name';
    name.addEventListener('input', () => { value.name = name.value; });
    const category = element('select');
    category.setAttribute('aria-label', translated('Skill category', appState.language)); category.dataset.originalLabel = 'Skill category';
    categories.forEach(key => { const option = uiElement('option', skillCategories[key] ?? key); option.value = key; category.append(option); });
    category.value = value.category;
    category.addEventListener('change', () => { value.category = category.value; });
    const remove = uiElement('button', 'Remove', 'secondary'); remove.type = 'button';
    remove.addEventListener('click', () => { draft.skills = draft.skills.filter(item => item !== value); row.remove(); });
    row.append(name, category, remove); byId('setup-skills').append(row);
  }
  function lockNewProfileTabs() {
    const locked = draft.developerId != null && String(draft.developerId) === appState.developerId && draft.job?.status !== 'COMPLETED';
    ['profile', 'retrieval', 'advisor'].forEach(name => {
      byId(`tab-${name}`).disabled = locked;
      byId(`tab-${name}`).setAttribute('aria-disabled', String(locked));
    });
    if (locked) activateTab('setup');
  }
  function renderJob(job) {
    if (!job || !job.jobId || !jobStatuses.includes(job.status) || String(job.devId) !== String(draft.developerId)) throw new Error('The server returned an unreadable ingestion response.');
    draft.job = job;
    const output = byId('setup-job'); output.replaceChildren();
    output.append(element('p', job.status, 'job-state'));
    const progress = element('progress');
    progress.setAttribute('aria-label', translated('Repository progress', appState.language)); progress.dataset.originalLabel = 'Repository progress';
    if (job.totalRepositories > 0) {
      progress.max = job.totalRepositories;
      progress.value = job.processedRepositories + job.skippedRepositories + job.failedRepositories;
    }
    if (job.status !== 'FAILED' && (job.totalRepositories > 0 || job.status !== 'COMPLETED')) output.append(progress);
    const counts = element('dl', null, 'job-counts');
    for (const [key, label] of Object.entries({ totalRepositories: 'Total repositories', processedRepositories: 'Processed', skippedRepositories: 'Skipped', failedRepositories: 'Failed', totalChunks: 'Chunks' })) {
      counts.append(uiElement('dt', label), element('dd', job[key]));
    }
    output.append(counts);
    if (job.currentRepository) output.append(uiElement('h4', 'Current repository'), element('p', job.currentRepository));
    if (job.nextAttemptAt) output.append(uiElement('h4', 'Next attempt'), element('p', job.nextAttemptAt));
    status('setup-job-status', job.status === 'COMPLETED' ? 'Ingestion complete. Profile, evidence and analysis tabs are ready.'
      : job.status === 'FAILED' ? 'Ingestion failed. Check the local backend configuration before retrying.' : 'Ingestion is running. Progress comes from the backend job.');
    byId('setup-check-job').hidden = ['COMPLETED', 'FAILED'].includes(job.status);
    byId('setup-start-job').hidden = job.status !== 'FAILED';
    lockNewProfileTabs();
  }
  async function checkJob() {
    clearTimeout(pollingTimer);
    if (!draft.job?.jobId) return;
    byId('setup-check-job').disabled = true;
    try {
      const job = await request(`/api/v1/ingestion-jobs/${encodeURIComponent(draft.job.jobId)}`);
      renderJob(job);
      if (!['COMPLETED', 'FAILED'].includes(job.status)) pollingTimer = setTimeout(checkJob, 3000);
    } catch (error) {
      status('setup-job-status', error.message, true);
      byId('setup-check-job').hidden = false;
    } finally { byId('setup-check-job').disabled = false; }
  }
  async function startJob() {
    if (!draft.developerId || byId('setup-start-job').disabled) return;
    byId('setup-start-job').disabled = true;
    try {
      const job = await request(`/api/v1/ingestion-jobs/developers/${draft.developerId}/public`, { method: 'POST' });
      renderJob(job);
      if (!['COMPLETED', 'FAILED'].includes(job.status)) { byId('setup-start-job').hidden = true; pollingTimer = setTimeout(checkJob, 3000); }
    } catch (error) { status('setup-job-status', error.message, true); byId('setup-start-job').hidden = false; }
    finally { byId('setup-start-job').disabled = false; }
  }
  byId('setup-mode').addEventListener('change', () => {
    draft.mode = byId('setup-mode').value;
    byId('setup-pdf-fields').hidden = draft.mode !== 'pdf';
  });
  byId('setup-file').addEventListener('change', () => { draft.reviewed = false; ++importVersion; importController?.abort(); });
  byId('setup-name').addEventListener('input', () => { draft.fullName = byId('setup-name').value; });
  byId('setup-username').addEventListener('input', () => {
    draft.githubUsername = byId('setup-username').value;
    ++importVersion; importController?.abort();
  });
  byId('setup-add-skill').addEventListener('click', () => {
    try { addSkill(); } catch (error) { status('setup-status', error.message, true); }
  });
  byId('setup-import').addEventListener('click', async () => {
    let githubUsername, file;
    try {
      githubUsername = username(); file = byId('setup-file').files?.[0];
      if (!file || !file.name.toLowerCase().endsWith('.pdf') || file.size <= 0 || file.size > 4 * 1024 * 1024) throw new Error('Choose a PDF between 1 byte and 4 MiB.');
    } catch (error) { status('setup-status', error.message, true); return; }
    const version = ++importVersion;
    const controller = new AbortController(); importController = controller;
    const timeout = setTimeout(() => controller.abort(), 90000);
    const form = new FormData(); form.append('file', file); form.append('githubUsername', githubUsername);
    byId('setup-import').disabled = true; byId('setup-save').disabled = true;
    status('setup-status', 'Extracting resume text. Nothing has been saved yet.');
    try {
      const data = await request('/api/v1/resumes/import', { method: 'POST', body: form, signal: controller.signal });
      if (version !== importVersion) return;
      if (!data || typeof data.fullName !== 'string' || !data.fullName.trim() || !Array.isArray(data.skills)
          || data.skills.length > 100 || data.skills.some(skill => !skill || !skill.name?.trim() || !categories.includes(skill.category))) throw new Error('The resume returned no valid profile data. Use manual entry or try another PDF.');
      byId('setup-name').value = data.fullName; draft.fullName = data.fullName;
      draft.skills = []; byId('setup-skills').replaceChildren(); data.skills.forEach(addSkill);
      draft.reviewed = true;
      status('setup-status', 'Review the extracted name and skills, then save the profile.');
    } catch (error) { if (version === importVersion) status('setup-status', error.name === 'AbortError' ? 'Resume import timed out. Try again or use manual entry.' : error.message, true); }
    finally {
      clearTimeout(timeout); byId('setup-file').value = ''; byId('setup-import').disabled = false; byId('setup-save').disabled = false;
    }
  });
  byId('setup-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (saving || draft.developerId != null) return;
    let body;
    try {
      const fullName = byId('setup-name').value.trim(); const githubUsername = username();
      if (!fullName || fullName.length > 60) throw new Error('Enter a full name with up to 60 characters.');
      if (draft.mode === 'pdf' && !draft.reviewed) throw new Error('Import and review the PDF before saving, or choose manual entry.');
      if (draft.skills.some(skill => !skill.name.trim() || skill.name.trim().length > 40 || !categories.includes(skill.category))) throw new Error('Each skill needs a name (up to 40 characters) and a valid category.');
      body = JSON.stringify({ fullName, githubUsername, skills: draft.skills.map(skill => ({ name: skill.name.trim(), category: skill.category })) });
    } catch (error) { status('setup-status', error.message, true); return; }
    saving = true; byId('setup-save').disabled = true;
    status('setup-status', 'Saving developer profile…');
    try {
      const profile = await request('/api/v1/developers', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
      if (!profile || !/^[1-9]\d*$/.test(String(profile.id))) throw new Error('The server did not return a developer ID.');
      draft.developerId = String(profile.id);
      byId('developer-id').value = draft.developerId; byId('developer-id').dispatchEvent(new Event('input'));
      byId('setup-profile-fields').disabled = true;
      status('setup-status', 'Profile saved. Starting public repository ingestion.');
      lockNewProfileTabs(); await startJob();
    } catch (error) { status('setup-status', error.message, true); }
    finally { saving = false; byId('setup-save').disabled = draft.developerId != null; }
  });
  byId('setup-start-job').addEventListener('click', startJob);
  byId('setup-check-job').addEventListener('click', checkJob);
  document.addEventListener('developer-changed', lockNewProfileTabs);
}
