'use strict';

// One local allowlist. Skill names remain exactly as supplied by the profile API.
// No URL or file path is ever derived directly from an unknown skill name.
const technologyLogos = [
  ['java', ['java', 'java 21', 'jdk', 'openjdk']],
  ['kotlin', ['kotlin']], ['python', ['python', 'python3']],
  ['spring', ['spring', 'spring boot', 'springboot']], ['springai', ['spring ai']],
  ['quarkus', ['quarkus']], ['fastapi', ['fastapi', 'fast api']],
  ['postgresql', ['postgresql', 'postgres', 'postgres sql']], ['mysql', ['mysql', 'my sql']],
  ['redis', ['redis']], ['supabase', ['supabase']],
  ['langchain', ['langchain', 'lang chain']], ['langchain4j.ico', ['langchain4j', 'langchain 4j']],
  ['googlegemini', ['gemini', 'google gemini']],
  ['docker', ['docker']], ['podman', ['podman']], ['git', ['git']],
  ['githubactions', ['github actions', 'githubactions']], ['jenkins', ['jenkins']],
  ['junit', ['junit', 'junit 5', 'junit5', 'junit jupiter']], ['jira', ['jira', 'atlassian jira']]
];
function technologyKey(name) {
  return String(name).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
}
const technologyLogoPaths = new Map(technologyLogos.flatMap(([file, aliases]) =>
  aliases.map(alias => [technologyKey(alias), `/logos/${file.endsWith('.ico') ? file : file + '.svg'}`])));
function technologyLogo(name) {
  const key = technologyKey(name);
  return technologyLogoPaths.get(key) ?? technologyLogoPaths.get(key.replace(/\d+(?:\d)*$/, '')) ?? null;
}
