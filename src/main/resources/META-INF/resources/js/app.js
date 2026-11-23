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
