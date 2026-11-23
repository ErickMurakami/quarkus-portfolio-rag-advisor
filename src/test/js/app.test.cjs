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
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this._text = ''; this.children = nodes; }
  setAttribute(name, value) { this.attributes[name] = value; }
  addEventListener(name, handler) { (this.listeners[name] ??= []).push(handler); }
  async emit(name, event = {}) {
    await Promise.all((this.listeners[name] ?? []).map(handler => handler({ preventDefault() {}, ...event })));
  }
  focus() { this.focused = true; }
  querySelectorAll(selector) {
    const matches = node => selector === '[type="submit"]' ? node.type === 'submit'
      : selector === '[data-question]' ? node.dataset.question != null
      : selector === '[name="mode"]:checked' ? node.name === 'mode' && node.checked : false;
    return this.children.flatMap(node => [...(matches(node) ? [node] : []), ...node.querySelectorAll(selector)]);
  }
  querySelector(selector) { return this.querySelectorAll(selector)[0] ?? null; }
}
function environment({ local = true, fetch = async () => ({ ok: true, json: async () => ({}) }) } = {}) {
  const nodes = new Map();
  for (const file of ['index.html', 'partials/advisor.html', 'partials/retrieval.html']) {
    const html = readFileSync(path.join(root, 'templates', file), 'utf8');
    for (const match of html.matchAll(/id="([^"]+)"/g)) nodes.set(match[1], new Node());
  }
  if (!local) nodes.delete('advisor-form');
  const button = value => { const node = new Node('button'); node.type = 'submit'; node.value = value; return node; };
  if (local) {
    const example = new Node('button'); example.dataset.question = 'Synthetic question';
    nodes.get('advisor-form').append(button('analyze'), example);
  }
  const mode = new Node('input'); mode.name = 'mode'; mode.value = 'FOCUSED'; mode.checked = true;
  nodes.get('retrieval-form').append(mode, button('search'), button('compare'));
  const document = new Node();
  document.getElementById = id => nodes.get(id) ?? null;
  document.createElement = tag => new Node(tag);
  document.documentElement = new Node('html');
  document.dispatchEvent = event => { void document.emit(event.type); };
  const timers = new Map(); let timerId = 0;
  const calls = [];
  const context = vm.createContext({
    document, Event, URL, AbortController, AbortSignal,
    matchMedia: () => ({ matches: false }),
    localStorage: { getItem: () => null, setItem() {} },
    setTimeout: (fn, delay) => { timers.set(++timerId, { fn, delay }); return timerId; },
    clearTimeout: id => timers.delete(id),
    fetch: (...args) => { calls.push(args); return fetch(...args); }
  });
  vm.runInContext(script, context);
  return {
    nodes, calls, document,
    get: id => nodes.get(id),
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
