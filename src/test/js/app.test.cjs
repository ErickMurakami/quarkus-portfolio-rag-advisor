// Dependency-free behavior checks for app.js. This small DOM double does not
// replace browser layout/accessibility testing. All API responses are synthetic.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '../../main/resources');
const script = readFileSync(path.join(root, 'META-INF/resources/js/app.js'), 'utf8');

class Node {
  constructor(tag = 'div') {
    this.tagName = tag; this.children = []; this.listeners = {};
    this.attributes = {}; this.dataset = {}; this.value = ''; this._text = '';
    this.disabled = false; this.classes = new Set();
    this.classList = { toggle: (name, on) => on ? this.classes.add(name) : this.classes.delete(name) };
  }
  set textContent(text) { this._text = String(text); this.children = []; }
  get textContent() { return this._text + this.children.map(node => node.textContent).join(' '); }
  set innerHTML(_) { throw new Error('Untrusted HTML rendering is forbidden'); }
  append(...nodes) { nodes.forEach(node => { node.parent = this; }); this.children.push(...nodes); }
  remove() { if (this.parent) this.parent.children = this.parent.children.filter(node => node !== this); }
  replaceChildren(...nodes) { this._text = ''; this.children = nodes; }
  setAttribute(name, value) { this.attributes[name] = value; if (name === 'src') this.src = value; }
  getAttribute(name) { return this.attributes[name] ?? null; }
  dispatchEvent(event) { void this.emit(event.type); }
  addEventListener(name, handler) { (this.listeners[name] ??= []).push(handler); }
  async emit(name, event = {}) {
    await Promise.all((this.listeners[name] ?? []).map(handler => handler({ preventDefault() {}, ...event })));
  }
  focus() { this.focused = true; }
  querySelectorAll(selector) {
    const matches = node => selector === '[type="submit"]' ? node.type === 'submit'
      : selector === '[data-question]' ? node.dataset.question != null
      : selector === '[name="mode"]:checked' ? node.name === 'mode' && node.checked
      : selector === '[name="mode"]' ? node.name === 'mode'
      : selector === '[data-i18n]' ? node.dataset.i18n != null
      : selector === '[data-i18n-placeholder]' ? node.dataset.i18nPlaceholder != null
      : selector === '[aria-label]' ? node.attributes['aria-label'] != null : false;
    return this.children.flatMap(node => [...(matches(node) ? [node] : []), ...node.querySelectorAll(selector)]);
  }
  querySelector(selector) { return this.querySelectorAll(selector)[0] ?? null; }
}
function environment({ local = true, language = 'en', url = 'http://localhost/', fetch = async () => ({ ok: true, json: async () => ({}) }) } = {}) {
  const nodes = new Map();
  for (const file of ['index.html', 'partials/advisor.html', 'partials/retrieval.html', 'partials/setup.html']) {
    const html = readFileSync(path.join(root, 'templates', file), 'utf8');
    for (const match of html.matchAll(/id="([^"]+)"/g)) nodes.set(match[1], new Node());
  }
  if (!local) { nodes.delete('advisor-form'); nodes.delete('setup-form'); }
  const button = value => { const node = new Node('button'); node.type = 'submit'; node.value = value; return node; };
  if (local) {
    const example = new Node('button'); example.dataset.question = 'Synthetic question';
    nodes.get('advisor-form').append(button('analyze'), example);
  }
  const mode = new Node('input'); mode.name = 'mode'; mode.value = 'FOCUSED'; mode.checked = true;
  nodes.get('retrieval-form').append(mode, button('search'), button('compare'));
  if (local) {
    nodes.get('setup-root').dataset.categories = 'LANGUAGE,FRAMEWORK,DATABASE,ARCHITECTURE,TOOL,OTHER';
    nodes.get('setup-save').type = 'submit';
    nodes.get('setup-form').append(nodes.get('setup-profile-fields'), nodes.get('setup-save'));
  }
  const document = new Node();
  document.children = [...nodes.values()];
  const window = new Node();
  window.location = { href: url };
  window.history = { replaceState: (_, __, value) => { window.location.href = String(value); } };
  document.getElementById = id => nodes.get(id) ?? null;
  document.createElement = tag => new Node(tag);
  document.documentElement = new Node('html');
  document.dispatchEvent = event => { void document.emit(event.type); };
  const timers = new Map(); let timerId = 0;
  const calls = [];
  const context = vm.createContext({
    document, window, Event, URL, AbortController, AbortSignal, FormData, File,
    matchMedia: () => ({ matches: false }),
    localStorage: { getItem: key => key === 'portfolio-language' ? language : null, setItem() {} },
    setTimeout: (fn, delay) => { timers.set(++timerId, { fn, delay }); return timerId; },
    clearTimeout: id => timers.delete(id),
    fetch: (...args) => { calls.push(args); return fetch(...args); }
  });
  vm.runInContext(readFileSync(path.join(root, 'META-INF/resources/js/i18n.js'), 'utf8'), context);
  vm.runInContext(readFileSync(path.join(root, 'META-INF/resources/js/technology-logos.js'), 'utf8'), context);
  vm.runInContext(script, context);
  vm.runInContext(readFileSync(path.join(root, 'META-INF/resources/js/setup.js'), 'utf8'), context);
  return {
    nodes, calls, document, window,
    logo: name => vm.runInContext(`technologyLogo(${JSON.stringify(name)})`, context),
    state: () => JSON.parse(vm.runInContext('JSON.stringify(appState)', context)),
    get: id => nodes.get(id),
    runTimer: async delay => {
      for (const [id, timer] of [...timers]) if (timer.delay === delay) { timers.delete(id); await timer.fn(); }
    },
    runDebounce: async () => {
      for (const [id, timer] of [...timers]) if (timer.delay === 350) { timers.delete(id); await timer.fn(); }
    }
  };
}
const response = data => ({ ok: true, json: async () => data });
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r; }); return { promise, resolve }; };
const submit = (env, form, value) => env.get(form).emit('submit', { submitter: { value } });
function selectDeveloper(env, id = '7') { env.get('developer-id').value = id; }
function allNodes(node) { return [node, ...node.children.flatMap(allNodes)]; }

test('profile loads the real contract and groups declared skills without hardcoded stack', async () => {
  const pending = deferred();
  const env = environment({ fetch: () => pending.promise });
  selectDeveloper(env);
  const request = env.get('profile-refresh').emit('click');
  assert.match(env.get('profile-status').textContent, /Loading/);
  pending.resolve(response({ id: 7, fullName: '<script>synthetic</script>', githubUsername: 'synthetic-user', skills: [
    { name: 'Example Language', category: 'LANGUAGE' }, { name: 'Example DB', category: 'DATABASE' }
  ] }));
  // Click starts an async load without blocking the event dispatcher.
  await request; await new Promise(setImmediate);
  assert.equal(env.calls[0][0], '/api/v1/developers/7');
  const text = env.get('profile-result').textContent;
  assert.match(text, /<script>synthetic<\/script>/);
  assert.match(text, /@synthetic-user/);
  assert.match(text, /Languages.*Example Language.*Databases.*Example DB/);
  assert.match(env.get('profile-status').textContent, /not verified/);
});

test('ID edits debounce, abort stale profiles and ignore late responses', async () => {
  const first = deferred(); const second = deferred();
  const env = environment({ fetch: url => url.endsWith('/1') ? first.promise : second.promise });
  selectDeveloper(env, '1');
  await env.get('developer-id').emit('input'); const firstLoading = env.runDebounce();
  selectDeveloper(env, '2');
  await env.get('developer-id').emit('input');
  assert.equal(env.calls[0][1].signal.aborted, true);
  // runDebounce awaits the handler; use its promise while completing fetch.
  const loading = env.runDebounce();
  second.resolve(response({ id: 2, fullName: 'Current developer', skills: [] }));
  await loading;
  first.resolve(response({ id: 1, fullName: 'Stale developer', skills: [] }));
  await firstLoading; await new Promise(setImmediate);
  assert.match(env.get('profile-result').textContent, /Current developer/);
  assert.doesNotMatch(env.get('profile-result').textContent, /Stale developer/);
  assert.match(env.get('profile-status').textContent, /No technical skills/);
});

test('invalid IDs never trigger an API request', async () => {
  for (const id of ['', '0', '-1', '1.5', '9223372036854775808', '<img>']) {
    const env = environment(); selectDeveloper(env, id);
    await env.get('profile-refresh').emit('click');
    assert.equal(env.calls.length, 0);
    assert.match(env.get('profile-status').textContent, /positive developer ID/);
  }
});

test('profile error and empty states do not expose server bodies', async () => {
  for (const data of [null, {}]) {
    const env = environment({ fetch: async () => response(data) }); selectDeveloper(env);
    await env.get('profile-refresh').emit('click'); await new Promise(setImmediate);
    assert.match(env.get('profile-status').textContent, /No developer profile/);
  }
  const env = environment({ fetch: async () => ({ ok: false, status: 500, text: () => 'PRIVATE SERVER DATA' }) });
  selectDeveloper(env); await env.get('profile-refresh').emit('click'); await new Promise(setImmediate);
  assert.match(env.get('profile-status').textContent, /could not complete/);
  assert.doesNotMatch(env.get('profile-status').textContent, /PRIVATE SERVER DATA/);
});

test('advisor posts trimmed question and renders safe text, priorities and sources', async () => {
  const env = environment({ fetch: async () => response({ answer: '<img src=x onerror=alert(1)>', strengths: ['Synthetic strength'],
    tips: [{ title: 'Synthetic tip', description: 'Improve documentation', priority: 'HIGH', relatedRepos: ['Private Repository 1'] }],
    sources: [{ repo: 'Private Repository 1', sourceReference: null, similarity: 0.9 },
      { repo: 'unsafe', sourceReference: 'javascript:alert(1)', similarity: 0.8 },
      { repo: 'public', sourceReference: 'https://github.com/synthetic/public', similarity: 0.7 }] }) });
  selectDeveloper(env); env.get('advisor-question').value = '  Synthetic question  ';
  await submit(env, 'advisor-form', 'analyze');
  assert.equal(env.calls[0][0], '/api/v1/developers/7/advisor');
  assert.deepEqual(JSON.parse(env.calls[0][1].body), { question: 'Synthetic question' });
  const result = env.get('advisor-result');
  assert.match(result.textContent, /<img src=x onerror=alert\(1\)>/);
  assert.match(result.textContent, /HIGH/);
  assert.match(result.textContent, /Cosine similarity 0.900/);
  const links = allNodes(result).filter(node => node.tagName === 'a');
  assert.equal(links.length, 1);
  assert.equal(links[0].href, 'https://github.com/synthetic/public');
  assert.equal(links[0].rel, 'noopener noreferrer');
});

test('advisor supports loading, empty analysis, empty lists and errors', async () => {
  const pending = deferred(); const env = environment({ fetch: () => pending.promise });
  selectDeveloper(env); env.get('advisor-question').value = 'Question';
  const waiting = submit(env, 'advisor-form', 'analyze');
  assert.equal(env.get('advisor-form').querySelector('[type="submit"]').disabled, true);
  assert.match(env.get('advisor-status').textContent, /Analyzing/);
  pending.resolve(response({ answer: 'No evidence found for the developer', strengths: [], tips: [], sources: [] }));
  await waiting;
  assert.match(env.get('advisor-result').textContent, /No recommendations/);
  assert.equal(env.get('advisor-form').querySelector('[type="submit"]').disabled, false);
  for (const payload of [null, { answer: '' }]) {
    const empty = environment({ fetch: async () => response(payload) }); selectDeveloper(empty);
    empty.get('advisor-question').value = 'Question'; await submit(empty, 'advisor-form');
    assert.match(empty.get('advisor-status').textContent, /No analysis/);
  }
  const failed = environment({ fetch: async () => ({ ok: false, status: 403 }) }); selectDeveloper(failed);
  failed.get('advisor-question').value = 'Question'; await submit(failed, 'advisor-form');
  assert.match(failed.get('advisor-status').textContent, /local development demo/);
});

test('comparison sends identical inputs and renders independent modes and counts', async () => {
  const env = environment({ fetch: async () => response([
    { repo: 'synthetic', sourceReference: 'https://github.com/synthetic/public', similarity: 0.81, content: '<script>synthetic</script>' },
    { repo: 'synthetic', sourceReference: 'https://github.com/synthetic/public', similarity: 0.72, content: 'Another chunk' }
  ]) });
  selectDeveloper(env); env.get('retrieval-question').value = 'Same question';
  await submit(env, 'retrieval-form', 'compare');
  assert.equal(env.calls.length, 2);
  assert.match(env.calls[0][0], /retrieval\?mode=FOCUSED$/);
  assert.match(env.calls[1][0], /retrieval\?mode=OVERVIEW$/);
  assert.equal(env.calls[0][1].body, env.calls[1][1].body);
  assert.match(env.get('retrieval-results').textContent, /2 chunks · 1 distinct repositories/);
  assert.match(env.get('retrieval-results').textContent, /<script>synthetic<\/script>/);
});

test('empty FOCUSED does not imply a cutoff; empty OVERVIEW explains the actual threshold', async () => {
  const env = environment({ fetch: async () => response([]) }); selectDeveloper(env);
  env.get('retrieval-question').value = 'Question'; await submit(env, 'retrieval-form', 'compare');
  const panels = env.get('retrieval-results').children;
  assert.doesNotMatch(panels[0].textContent, /threshold/);
  assert.match(panels[1].textContent, /threshold \(0.65\)/);
});

test('comparison preserves successful results when the other mode fails', async () => {
  const env = environment({ fetch: async url => url.endsWith('OVERVIEW') ? { ok: false, status: 429 } : response([]) });
  selectDeveloper(env); env.get('retrieval-question').value = 'Question';
  await submit(env, 'retrieval-form', 'compare');
  assert.match(env.get('retrieval-results').textContent, /No public evidence was found/);
  assert.match(env.get('retrieval-results').textContent, /service is busy/);
  assert.match(env.get('retrieval-status').textContent, /could not complete/);
});

test('changing developer cancels analysis and evidence and clears private output', async () => {
  const pending = deferred(); const env = environment({ fetch: () => pending.promise }); selectDeveloper(env);
  env.get('advisor-question').value = 'Question'; env.get('retrieval-question').value = 'Question';
  const analysis = submit(env, 'advisor-form'); const evidence = submit(env, 'retrieval-form');
  env.get('advisor-result').append(new Node('p'));
  selectDeveloper(env, '8'); await env.get('developer-id').emit('input');
  assert.equal(env.get('advisor-result').children.length, 0);
  assert.equal(env.get('retrieval-results').children.length, 0);
  assert.ok(env.calls.every(([, options]) => options.signal.aborted));
  pending.resolve(response([])); await Promise.all([analysis, evidence]);
  assert.equal(env.get('retrieval-results').children.length, 0);
});

test('production page without advisor form still supports theme and retrieval', async () => {
  const env = environment({ local: false });
  await env.get('theme-toggle').emit('click');
  assert.equal(env.document.documentElement.dataset.theme, 'light');
  assert.equal(env.calls.length, 0);
  assert.match(env.get('theme-toggle').textContent, /Dark theme/);
});

test('switching tabs preserves form values, API results and shared developer selection', async () => {
  const env = environment({ fetch: async () => response({ answer: 'Synthetic analysis', strengths: [], tips: [], sources: [] }) });
  selectDeveloper(env, '12');
  env.get('advisor-question').value = 'Saved question';
  await env.get('advisor-question').emit('input');
  await submit(env, 'advisor-form');
  const result = env.get('advisor-result').children[0];
  for (const tab of ['retrieval', 'profile', 'advisor']) await env.get(`tab-${tab}`).emit('click');
  assert.equal(env.get('advisor-result').children[0], result);
  assert.equal(env.get('advisor-question').value, 'Saved question');
  assert.equal(env.calls.length, 1);
  assert.equal(env.state().activeTab, 'advisor');
  assert.equal(env.state().developerId, '12');
  assert.equal(env.state().advisor.answer, 'Synthetic analysis');
  assert.equal(env.state().forms.advisorQuestion, 'Saved question');
  assert.equal(env.get('advisor').hidden, false);
  assert.equal(env.get('profile').hidden, true);
});

test('tabs support arrow keys, Home and End with managed focus', async () => {
  const env = environment();
  await env.get('tab-profile').emit('keydown', { key: 'ArrowRight' });
  assert.equal(env.state().activeTab, 'retrieval');
  assert.equal(env.get('tab-retrieval').focused, true);
  assert.equal(env.get('tab-profile').attributes['aria-selected'], 'false');
  await env.get('tab-retrieval').emit('keydown', { key: 'End' });
  assert.equal(env.state().activeTab, 'setup');
  await env.get('tab-setup').emit('keydown', { key: 'Home' });
  assert.equal(env.state().activeTab, 'profile');
});

test('developerId URL selection loads the real profile and retains unrelated query parameters', async () => {
  const env = environment({ url: 'http://localhost/?developerId=21&view=presentation',
    fetch: async () => response({ id: 21, fullName: 'Synthetic Presenter', githubUsername: 'synthetic', skills: [] }) });
  await env.runDebounce();
  assert.equal(env.calls[0][0], '/api/v1/developers/21');
  assert.equal(env.get('developer-id').value, '21');
  assert.equal(env.state().profile.fullName, 'Synthetic Presenter');
  assert.match(env.get('selected-developer').textContent, /Synthetic Presenter · @synthetic/);
  selectDeveloper(env, '22'); await env.get('developer-id').emit('input');
  assert.match(env.window.location.href, /developerId=22/);
  assert.match(env.window.location.href, /view=presentation/);
  assert.equal(env.state().profile, null);
  const unsupported = environment({ url: 'http://localhost/?github=synthetic' });
  await unsupported.runDebounce();
  assert.equal(unsupported.calls.length, 0);
});

test('Portuguese is the default; changing language preserves unmodified API content and avoids refetching', async () => {
  const env = environment({ language: null, fetch: async () => response({ answer: 'Analysis', strengths: ['Tools'], tips: [], sources: [] }) });
  assert.equal(env.document.documentElement.lang, 'pt-BR');
  assert.equal(env.get('language-flag').src, '/flags/brazil.svg');
  assert.match(env.get('language-toggle').attributes['aria-label'], /Mudar para inglês/);
  selectDeveloper(env); env.get('advisor-question').value = 'Minha pergunta';
  await submit(env, 'advisor-form');
  assert.match(env.get('advisor-result').textContent, /Pontos fortes demonstrados/);
  assert.ok(allNodes(env.get('advisor-result')).some(node => node.tagName === 'p' && node.textContent === 'Analysis'));
  assert.match(env.get('advisor-result').textContent, /Tools/);
  await env.get('language-toggle').emit('click');
  assert.equal(env.document.documentElement.lang, 'en');
  assert.equal(env.get('language-flag').src, '/flags/united-kingdom.svg');
  assert.match(env.get('language-toggle').attributes['aria-label'], /Switch to Portuguese/);
  assert.match(env.get('advisor-result').textContent, /Demonstrated strengths/);
  assert.equal(env.calls.length, 1);
  assert.equal(env.get('advisor-question').value, 'Minha pergunta');
  await env.get('language-toggle').emit('click');
  assert.match(env.get('advisor-status').textContent, /Análise concluída/);
  assert.equal(env.state().language, 'pt');
});

test('profile keeps uneven skill categories and original skill names without adding technologies', async () => {
  const skills = [{ name: 'Java', category: 'LANGUAGE' }, { name: 'Kotlin', category: 'LANGUAGE' },
    { name: 'Python', category: 'LANGUAGE' }, { name: 'Quarkus', category: 'FRAMEWORK' },
    { name: 'Custom internal tool', category: 'OTHER' }];
  const env = environment({ fetch: async () => response({ id: 7, fullName: 'Synthetic Developer', githubUsername: 'synthetic', skills }) });
  selectDeveloper(env); await env.get('profile-refresh').emit('click'); await new Promise(setImmediate);
  const groups = allNodes(env.get('profile-result')).filter(node => node.className === 'skill-group');
  assert.equal(groups.length, 3);
  assert.match(groups[0].textContent, /Languages.*Java.*Kotlin.*Python/);
  assert.match(groups[1].textContent, /Frameworks.*Quarkus/);
  assert.match(groups[2].textContent, /Other.*Custom internal tool/);
  assert.doesNotMatch(env.get('profile-result').textContent, /PostgreSQL/);
  assert.deepEqual(env.state().profile.skills, skills);
});

test('technology logos tolerate aliases and versions while unknown names stay text only', async () => {
  const env = environment({ fetch: async () => response({ id: 7, fullName: 'Synthetic', skills: [
    { name: '  JAVA 21 ', category: 'LANGUAGE' }, { name: 'spring-boot', category: 'FRAMEWORK' },
    { name: 'Private custom tool <img>', category: 'TOOL' }] }) });
  assert.equal(env.logo('PostgreSQL'), '/logos/postgresql.svg');
  assert.equal(env.logo('postgres'), '/logos/postgresql.svg');
  assert.equal(env.logo('JUnit 5'), '/logos/junit.svg');
  assert.equal(env.logo('Spring AI'), '/logos/springai.svg');
  assert.equal(env.logo('../logos/evil'), null);
  assert.equal(env.logo('Mockito'), null);
  selectDeveloper(env); await env.get('profile-refresh').emit('click'); await new Promise(setImmediate);
  const images = allNodes(env.get('profile-result')).filter(node => node.tagName === 'img');
  assert.equal(images.length, 2);
  assert.equal(images[0].alt, '');
  assert.equal(images[0].attributes['aria-hidden'], 'true');
  await images[0].emit('error'); assert.equal(images[0].hidden, true);
  assert.match(env.get('profile-result').textContent, /  JAVA 21 /);
  assert.match(env.get('profile-result').textContent, /Private custom tool <img>/);
});

test('initial locale is selected before app startup and unchanged labels retain their text nodes', async () => {
  const env = environment({ language: 'en' });
  assert.equal(env.document.documentElement.lang, 'en');
  assert.equal(env.document.documentElement.dataset.languagePending, undefined);
  const status = env.get('profile-status');
  status.dataset.i18n = 'Loading developer profile…';
  status.textContent = 'Loading developer profile…';
  const marker = new Node('span'); marker.textContent = ' retained child'; status.children.push(marker);
  // A generated result without data-i18n must never become translated UI copy.
  const result = new Node('p'); result.textContent = 'Analysis'; env.get('profile-result').append(result);
  await env.get('language-toggle').emit('click');
  assert.equal(env.document.documentElement.lang, 'pt-BR');
  assert.equal(status.textContent, 'Carregando perfil do desenvolvedor…');
  assert.equal(result.textContent, 'Analysis');
  assert.equal(env.calls.length, 0);
});

const pendingJob = { jobId: 'synthetic-job', devId: 42, status: 'PENDING', totalRepositories: 0, processedRepositories: 0, skippedRepositories: 0, failedRepositories: 0, totalChunks: 0, currentRepository: null, nextAttemptAt: null };
function setupIdentity(env) { env.get('setup-name').value = 'Synthetic Developer'; env.get('setup-username').value = 'synthetic'; }

test('manual setup creates a reviewed profile, stores its ID, starts only public ingestion and polls real counters', async () => {
  const env = environment({ fetch: async (url, options) => {
    if (url === '/api/v1/developers') return response({ id: 42 });
    if (url.endsWith('/public')) return response(pendingJob);
    if (url.endsWith('/synthetic-job')) return response({ ...pendingJob, status: 'COMPLETED', totalRepositories: 3, processedRepositories: 2, skippedRepositories: 1, totalChunks: 9 });
    throw new Error('Unexpected API request');
  } });
  setupIdentity(env);
  await env.get('setup-add-skill').emit('click');
  const row = env.get('setup-skills').children[0];
  row.children[0].value = 'Java'; await row.children[0].emit('input');
  row.children[1].value = 'LANGUAGE'; await row.children[1].emit('change');
  await submit(env, 'setup-form');
  const saved = JSON.parse(env.calls[0][1].body);
  assert.deepEqual(saved, { fullName: 'Synthetic Developer', githubUsername: 'synthetic', skills: [{ name: 'Java', category: 'LANGUAGE' }] });
  assert.equal(env.calls[1][0], '/api/v1/ingestion-jobs/developers/42/public');
  assert.match(env.window.location.href, /developerId=42/);
  assert.equal(env.get('tab-retrieval').disabled, true);
  assert.equal(env.state().activeTab, 'setup');
  assert.match(env.get('setup-job').textContent, /PENDING/);
  await env.runTimer(3000);
  assert.equal(env.get('tab-retrieval').disabled, false);
  assert.equal(env.get('tab-profile').disabled, false);
  assert.equal(env.state().setup.job.totalChunks, 9);
  assert.match(env.get('setup-job').textContent, /COMPLETED/);
  assert.ok(env.calls.every(([url]) => !url.includes('/github/me')));
});

test('PDF import uses multipart and returns editable skills before any profile is created', async () => {
  const env = environment({ fetch: async url => url === '/api/v1/developers' ? response({ id: 42 }) : url.endsWith('/public') ? response(pendingJob) : response({ fullName: 'Extracted Name', githubUsername: 'synthetic', skills: [{ name: 'Java', category: 'LANGUAGE' }, { name: 'Wrong skill', category: 'OTHER' }] }) });
  setupIdentity(env); env.get('setup-mode').value = 'pdf'; await env.get('setup-mode').emit('change');
  env.get('setup-file').files = [new File(['%PDF-synthetic'], 'resume.pdf', { type: 'application/pdf' })];
  await env.get('setup-import').emit('click');
  assert.equal(env.calls.length, 1); assert.equal(env.calls[0][0], '/api/v1/resumes/import');
  assert.equal(env.calls[0][1].body.get('githubUsername'), 'synthetic');
  assert.equal(env.calls[0][1].headers, undefined);
  assert.equal(env.get('setup-name').value, 'Extracted Name');
  assert.match(env.get('setup-status').textContent, /Review the extracted/);
  const rows = env.get('setup-skills').children;
  rows[0].children[0].value = 'Corrected Java'; await rows[0].children[0].emit('input');
  await rows[1].children[2].emit('click');
  await env.get('setup-add-skill').emit('click');
  assert.equal(env.state().setup.skills[0].name, 'Corrected Java');
  assert.equal(env.state().setup.skills.length, 2);
  await env.get('tab-profile').emit('click'); await env.get('tab-setup').emit('click');
  await env.get('language-toggle').emit('click');
  assert.equal(env.get('setup-name').value, 'Extracted Name');
  assert.equal(rows[0].children[0].value, 'Corrected Java');
  assert.equal(env.calls.length, 1);
  const added = env.get('setup-skills').children[1];
  added.children[0].value = 'Redis'; await added.children[0].emit('input');
  added.children[1].value = 'DATABASE'; await added.children[1].emit('change');
  await submit(env, 'setup-form');
  assert.deepEqual(JSON.parse(env.calls[1][1].body).skills, [{ name: 'Corrected Java', category: 'LANGUAGE' }, { name: 'Redis', category: 'DATABASE' }]);
});

test('setup rejects absent and oversized PDFs and missing manual data without API calls', async () => {
  const env = environment();
  await submit(env, 'setup-form'); assert.equal(env.calls.length, 0);
  setupIdentity(env);
  await env.get('setup-import').emit('click'); assert.equal(env.calls.length, 0);
  env.get('setup-file').files = [{ name: 'large.pdf', size: 4 * 1024 * 1024 + 1 }];
  await env.get('setup-import').emit('click'); assert.equal(env.calls.length, 0);
  env.get('setup-mode').value = 'pdf'; await env.get('setup-mode').emit('change');
  await submit(env, 'setup-form'); assert.equal(env.calls.length, 0);
  assert.match(env.get('setup-status').textContent, /Import and review/);
});

test('setup reports failed PDF extraction and never saves an invalid extracted profile', async () => {
  for (const result of [response(null), response({ fullName: '', skills: [] }), { ok: false, status: 400 }]) {
    const env = environment({ fetch: async () => result }); setupIdentity(env);
    env.get('setup-file').files = [new File(['%PDF-test'], 'resume.pdf')];
    await env.get('setup-import').emit('click');
    assert.equal(env.state().setup.reviewed, false);
    assert.equal(env.calls.length, 1);
    assert.equal(env.get('setup-import').disabled, false);
    assert.ok(env.get('setup-status').classes.has('error'));
  }
});

test('setup preserves a saved ID if ingestion cannot start and retry never creates another profile', async () => {
  let tries = 0;
  const env = environment({ fetch: async url => {
    if (url === '/api/v1/developers') return response({ id: 42 });
    if (++tries === 1) return { ok: false, status: 500 };
    return response({ ...pendingJob, status: 'FAILED' });
  } }); setupIdentity(env);
  await submit(env, 'setup-form');
  assert.equal(env.state().setup.developerId, '42');
  assert.equal(env.get('setup-start-job').hidden, false);
  await env.get('setup-start-job').emit('click');
  assert.match(env.get('setup-job').textContent, /FAILED/);
  assert.equal(env.get('tab-advisor').disabled, true);
  assert.equal(env.calls.filter(([url]) => url === '/api/v1/developers').length, 1);
});

test('waiting-for-rate-limit is reported from the job without re-posting ingestion', async () => {
  const env = environment({ fetch: async url => url === '/api/v1/developers' ? response({ id: 42 })
    : url.endsWith('/public') ? response(pendingJob)
    : response({ ...pendingJob, status: 'WAITING_RATE_LIMIT', totalRepositories: 4, currentRepository: 'synthetic-public', nextAttemptAt: '2026-11-24T12:00:00Z' }) });
  setupIdentity(env); await submit(env, 'setup-form'); await env.runTimer(3000);
  assert.match(env.get('setup-job').textContent, /WAITING_RATE_LIMIT/);
  assert.match(env.get('setup-job').textContent, /2026-11-24T12:00:00Z/);
  assert.equal(env.calls.filter(([url]) => url.endsWith('/public')).length, 1);
});
