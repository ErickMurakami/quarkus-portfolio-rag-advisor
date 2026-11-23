'use strict';

const byId = id => document.getElementById(id);
const list = value => Array.isArray(value) ? value : [];
function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text != null) node.textContent = String(text);
  if (className) node.className = className;
  return node;
}
function status(id, message, error = false) {
  const node = byId(id);
  node.textContent = message;
  node.classList.toggle('error', error);
}
function developerId() {
  const id = byId('developer-id').value.trim();
  if (!/^[1-9]\d*$/.test(id) || BigInt(id) > 9223372036854775807n) {
    throw new Error('Enter a positive developer ID within the supported range.');
  }
  return id;
}
async function request(url, options = {}) {
  let response;
  try { response = await fetch(url, { ...options, signal: options.signal ?? AbortSignal.timeout(90000) }); }
  catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('Unable to reach the server. Check the connection and try again.');
  }
  if (!response.ok) {
    const messages = {
      400: 'The request was rejected. Check the developer ID and question.',
      403: 'Advisor analysis is available only in the local development demo.',
      404: 'The requested developer or resource was not found.',
      429: 'The service is busy. Please wait before trying again.'
    };
    // Never display server error bodies: they can include internal or private data.
    throw new Error(messages[response.status] ?? 'The server could not complete the request. Check that the developer exists and the backend is configured.');
  }
  try { return await response.json(); }
  catch { throw new Error('The server returned an unreadable response. Please try again.'); }
}
function sourceLink(name, reference) {
  const fallback = element('span', name);
  if (!reference) return fallback;
  try {
    const url = new URL(reference);
    if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password) return fallback;
    const link = element('a', name);
    link.href = url.href;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.referrerPolicy = 'no-referrer';
    return link;
  } catch { return fallback; }
}
function tags(values) {
  const container = element('div', null, 'tags');
  list(values).forEach(value => container.append(element('span', value)));
  return container;
}

const themeToggle = byId('theme-toggle');
function setTheme(theme) {
  document.documentElement.dataset.theme = theme;
  const label = theme === 'dark' ? 'Light theme' : 'Dark theme';
  themeToggle.textContent = label;
  themeToggle.setAttribute('aria-label', `Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`);
}
let savedTheme;
try { savedTheme = localStorage.getItem('portfolio-theme'); } catch { /* Storage is optional. */ }
setTheme(['light', 'dark'].includes(savedTheme) ? savedTheme : (matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'));
themeToggle.addEventListener('click', () => {
  const theme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  setTheme(theme);
  try { localStorage.setItem('portfolio-theme', theme); } catch { /* Keep the session theme. */ }
});

const skillCategories = {
  LANGUAGE: 'Languages', FRAMEWORK: 'Frameworks', DATABASE: 'Databases',
  ARCHITECTURE: 'Architecture', TOOL: 'Tools', OTHER: 'Other'
};
let profileController;
let profileVersion = 0;
async function loadProfile() {
  const version = ++profileVersion;
  profileController?.abort();
  byId('profile-result').replaceChildren();
  byId('profile-result').setAttribute('aria-busy', 'false');
  let id;
  try { id = developerId(); }
  catch (error) { status('profile-status', error.message, true); return; }
  const controller = new AbortController();
  profileController = controller;
  const timeout = setTimeout(() => controller.abort(), 30000);
  status('profile-status', 'Loading developer profile…');
  byId('profile-result').setAttribute('aria-busy', 'true');
  try {
    const profile = await request(`/api/v1/developers/${id}`, { signal: controller.signal });
    if (version !== profileVersion) return;
    if (!profile || profile.id == null) { status('profile-status', 'No developer profile was returned.'); return; }
    const result = byId('profile-result');
    result.append(element('h3', profile.fullName || 'Unnamed developer'));
    result.append(element('p', profile.githubUsername ? `@${profile.githubUsername}` : 'No GitHub username declared.', 'muted'));
    const skills = list(profile.skills).filter(Boolean);
    for (const [category, label] of Object.entries(skillCategories)) {
      const names = skills.filter(skill => skill.category === category).map(skill => skill.name);
      if (names.length) result.append(element('h4', label), tags(names));
    }
    status('profile-status', skills.length ? 'Declared base stack · not verified by repository evidence' : 'No technical skills have been declared.');
  } catch (error) {
    if (version === profileVersion) status('profile-status', error.name === 'AbortError' ? 'Profile request timed out. Try refreshing.' : error.message, true);
  } finally {
    clearTimeout(timeout);
    if (version === profileVersion) byId('profile-result').setAttribute('aria-busy', 'false');
  }
}
let profileTimer;
byId('developer-id').addEventListener('input', () => {
  clearTimeout(profileTimer);
  ++profileVersion;
  profileController?.abort();
  byId('profile-result').replaceChildren();
  status('profile-status', 'Waiting for developer ID…');
  document.dispatchEvent(new Event('developer-changed'));
  profileTimer = setTimeout(loadProfile, 350);
});
byId('profile-refresh').addEventListener('click', () => { clearTimeout(profileTimer); loadProfile(); });

function questionBody(id) {
  const question = byId(id).value.trim();
  if (!question) throw new Error('Enter a question before continuing.');
  return JSON.stringify({ question });
}
function similarity(value) {
  return typeof value === 'number' && Number.isFinite(value) ? `Cosine similarity ${value.toFixed(3)}` : 'Similarity unavailable';
}
function renderAdvisor(data) {
  const result = byId('advisor-result');
  if (!data || typeof data.answer !== 'string' || !data.answer.trim()) {
    status('advisor-status', 'No analysis was returned. Try a different question.');
    return;
  }
  const answer = element('article', null, 'card');
  answer.append(element('h3', 'Analysis'), element('p', data.answer, 'prose'));
  result.append(answer);
  const strengths = element('article', null, 'card');
  strengths.append(element('h3', 'Demonstrated strengths'));
  const items = element('ul');
  list(data.strengths).forEach(value => items.append(element('li', value)));
  strengths.append(items.children.length ? items : element('p', 'No strengths were returned.', 'muted'));
  result.append(strengths);
  const tips = element('div', null, 'result-grid');
  list(data.tips).filter(Boolean).forEach(tip => {
    const card = element('article', null, 'card');
    const heading = element('div', null, 'result-title');
    heading.append(element('h3', tip.title || 'Recommendation'));
    if (['HIGH', 'MEDIUM', 'LOW'].includes(tip.priority)) {
      heading.append(element('span', tip.priority, `badge priority-${tip.priority.toLowerCase()}`));
    }
    card.append(heading, element('p', tip.description, 'prose'));
    if (list(tip.relatedRepos).length) card.append(tags(tip.relatedRepos));
    tips.append(card);
  });
  result.append(tips.children.length ? tips : element('p', 'No recommendations were returned.', 'note'));
  const sources = element('article', null, 'card');
  sources.append(element('h3', 'Sources'));
  list(data.sources).filter(Boolean).forEach(source => {
    const row = element('div', null, 'source-row');
    row.append(sourceLink(source.repo, source.sourceReference), element('span', similarity(source.similarity), 'similarity'));
    if (!source.sourceReference) row.append(element('span', 'Source without a public link', 'muted'));
    sources.append(row);
  });
  if (!list(data.sources).length) sources.append(element('p', 'No evidence sources were returned.', 'muted'));
  result.append(sources);
  status('advisor-status', 'Analysis complete. Review recommendations against the source evidence.');
}
const advisorForm = byId('advisor-form');
let advisorController;
let advisorVersion = 0;
if (advisorForm) {
  advisorForm.querySelectorAll('[data-question]').forEach(button => button.addEventListener('click', () => {
    byId('advisor-question').value = button.dataset.question;
    byId('advisor-question').focus();
  }));
  advisorForm.addEventListener('submit', async event => {
    event.preventDefault();
    let id, body;
    try { id = developerId(); body = questionBody('advisor-question'); }
    catch (error) { status('advisor-status', error.message, true); return; }
    const version = ++advisorVersion;
    advisorController?.abort();
    const controller = new AbortController();
    advisorController = controller;
    const timeout = setTimeout(() => controller.abort(), 90000);
    const button = advisorForm.querySelector('[type="submit"]');
    button.disabled = true;
    button.textContent = 'Analyzing…';
    byId('advisor-result').replaceChildren();
    byId('advisor-result').setAttribute('aria-busy', 'true');
    status('advisor-status', 'Analyzing repository evidence…');
    try {
      const data = await request(`/api/v1/developers/${id}/advisor`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body, signal: controller.signal
      });
      if (version === advisorVersion) renderAdvisor(data);
    } catch (error) {
      if (version === advisorVersion) status('advisor-status', error.name === 'AbortError' ? 'Analysis timed out. Please try again.' : error.message, true);
    } finally {
      clearTimeout(timeout);
      if (version === advisorVersion) {
        button.disabled = false;
        button.textContent = 'Analyze portfolio';
        byId('advisor-result').setAttribute('aria-busy', 'false');
      }
    }
  });
  document.addEventListener('developer-changed', () => {
    ++advisorVersion;
    advisorController?.abort();
    byId('advisor-result').replaceChildren();
    byId('advisor-result').setAttribute('aria-busy', 'false');
    const button = advisorForm.querySelector('[type="submit"]');
    button.disabled = false;
    button.textContent = 'Analyze portfolio';
    status('advisor-status', 'Developer changed. Run a new analysis.');
  });
}

let retrievalController;
let retrievalVersion = 0;
const retrievalForm = byId('retrieval-form');
function clearRetrieval() {
  ++retrievalVersion;
  retrievalController?.abort();
  byId('retrieval-results').replaceChildren();
  byId('retrieval-results').setAttribute('aria-busy', 'false');
  retrievalForm.querySelectorAll('[type="submit"]').forEach(button => { button.disabled = false; });
  status('retrieval-status', 'Inputs changed. Search again to refresh evidence.');
}
function renderEvidence(panel, chunks, mode) {
  if (!Array.isArray(chunks)) throw new Error('The server returned an unreadable evidence response.');
  if (!chunks.length) {
    panel.append(element('p', mode === 'OVERVIEW'
      ? 'No public evidence met the OVERVIEW similarity threshold (0.65). The developer may also have no indexed public documents.'
      : 'No public evidence was found. This developer may have no indexed public documents.', 'notice'));
    return;
  }
  const repositories = new Set(chunks.map(chunk => chunk.sourceReference));
  panel.append(element('p', `${chunks.length} chunks · ${repositories.size} distinct repositories`, 'note'));
  chunks.forEach(chunk => {
    const card = element('article', null, 'card');
    const heading = element('h3');
    heading.append(sourceLink(chunk.repo, chunk.sourceReference));
    card.append(heading, element('span', similarity(chunk.similarity), 'similarity'));
    const details = element('details');
    details.open = true;
    details.append(element('summary', 'Evidence chunk'), element('p', chunk.content, 'prose'));
    card.append(details);
    panel.append(card);
  });
}
retrievalForm.addEventListener('submit', async event => {
  event.preventDefault();
  let id, body;
  try { id = developerId(); body = questionBody('retrieval-question'); }
  catch (error) { status('retrieval-status', error.message, true); return; }
  const modes = event.submitter?.value === 'compare' ? ['FOCUSED', 'OVERVIEW'] : [retrievalForm.querySelector('[name="mode"]:checked').value];
  const version = ++retrievalVersion;
  retrievalController?.abort();
  const controller = new AbortController();
  retrievalController = controller;
  const timeout = setTimeout(() => controller.abort(), 90000);
  const results = byId('retrieval-results');
  results.replaceChildren();
  results.className = modes.length === 2 ? 'comparison' : '';
  results.setAttribute('aria-busy', 'true');
  retrievalForm.querySelectorAll('[type="submit"]').forEach(button => { button.disabled = true; });
  status('retrieval-status', `Searching public evidence for developer ${id}…`);
  const outcomes = await Promise.all(modes.map(async mode => {
    const panel = element('section', null, 'evidence-panel');
    panel.setAttribute('aria-label', `${mode} results`);
    panel.append(element('h3', mode));
    results.append(panel);
    try {
      const chunks = await request(`/api/v1/developers/${id}/retrieval?mode=${mode}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body, signal: controller.signal
      });
      if (version !== retrievalVersion) return false;
      renderEvidence(panel, chunks, mode);
      return true;
    } catch (error) {
      if (version === retrievalVersion) panel.append(element('p', error.name === 'AbortError' ? 'Search timed out. Please try again.' : error.message, 'error'));
      return false;
    }
  }));
  clearTimeout(timeout);
  if (version === retrievalVersion) {
    results.setAttribute('aria-busy', 'false');
    retrievalForm.querySelectorAll('[type="submit"]').forEach(button => { button.disabled = false; });
    status('retrieval-status', outcomes.every(Boolean) ? `Search complete · developer ${id} · public sources only` : 'Some evidence searches could not complete. Review the message for each mode.', !outcomes.every(Boolean));
  }
});
byId('retrieval-question').addEventListener('input', clearRetrieval);
document.addEventListener('developer-changed', clearRetrieval);
