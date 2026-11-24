'use strict';

// This small script is parser-blocking in <head>. Preferences are selected before
// CSS can paint; only untranslated labels are withheld until the body-end call.
const initialUiLanguage = (() => {
  try { return localStorage.getItem('portfolio-language') === 'en' ? 'en' : 'pt'; }
  catch { return 'pt'; }
})();
document.documentElement.lang = initialUiLanguage === 'pt' ? 'pt-BR' : 'en';
document.documentElement.dataset.languagePending = 'true';
let initialUiTheme;
try { initialUiTheme = localStorage.getItem('portfolio-theme'); } catch { /* Optional storage. */ }
document.documentElement.dataset.theme = ['light', 'dark'].includes(initialUiTheme)
  ? initialUiTheme : (matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');

// UI copy only. Developer data, evidence and generated analyses are never translated.
const portuguese = {
  'Skip to content': 'Ir para o conteúdo',
  'Built with Quarkus': 'Feito com Quarkus',
  'Profile': 'Perfil', 'Retrieval Explorer': 'Explorador de evidências', 'Portfolio Advisor': 'Análise do portfólio',
  'Refresh': 'Atualizar',
  'Developer ID': 'ID do desenvolvedor', 'Refresh profile': 'Atualizar perfil',
  'Enter an existing ID': 'Informe um ID existente', 'No developer selected': 'Nenhum desenvolvedor selecionado',
  'One developer, shared across all tabs.': 'Um desenvolvedor, compartilhado entre todas as abas.',
  'Engineering, backed by evidence': 'Engenharia, fundamentada em evidências',
  'Explore your engineering portfolio through semantic search and AI-powered analysis.': 'Explore seu portfólio de engenharia com busca semântica e análise por inteligência artificial.',
  'Technical stack': 'Stack técnica',
  'Developer Profile': 'Perfil do desenvolvedor', 'Declared skills': 'Competências declaradas',
  'Enter a developer ID to load their profile.': 'Informe o ID de um desenvolvedor para carregar seu perfil.',
  'The base stack is declared by the developer. These skills are not verified GitHub evidence. The application technologies describe this tool, not the developer.': 'A stack base é declarada pelo desenvolvedor. Essas competências não são evidências verificadas no GitHub. As tecnologias da aplicação descrevem esta ferramenta, não o desenvolvedor.',
  'Inspect the evidence': 'Explore as evidências', 'Public sources only': 'Somente fontes públicas',
  'From evidence to direction': 'Das evidências às recomendações', 'Local demo only': 'Somente demonstração local',
  'Evidence before assumptions.': 'Evidências antes de suposições.',
  'Light theme': 'Tema claro', 'Dark theme': 'Tema escuro',
  'Switch to light theme': 'Alternar para tema claro', 'Switch to dark theme': 'Alternar para tema escuro',
  'Workspace tabs': 'Abas de navegação', 'Application technologies': 'Tecnologias da aplicação',
  'Advisor controls': 'Controles da análise', 'Advisor results': 'Resultados da análise',
  'Retrieval controls': 'Controles da busca', 'Retrieval results': 'Resultados da busca',
  'Example advisor questions': 'Exemplos de perguntas',
  'Local demonstration only. Analysis can use private repository content. Repository aliases do not prevent private information from appearing in generated text. Use only data you are authorized to send to the AI provider.': 'Somente demonstração local. A análise pode utilizar conteúdo de repositórios privados. Os aliases não impedem que informações privadas apareçam no texto gerado. Utilize apenas dados que você tem autorização para enviar ao provedor de IA.',
  'What would you like to improve?': 'O que você gostaria de melhorar?',
  'How can I better demonstrate my backend engineering skills?': 'Como posso demonstrar melhor minhas competências em engenharia backend?',
  'Uses the developer ID selected above. Your question and retrieved evidence are sent to the configured AI provider.': 'Utiliza o ID selecionado acima. Sua pergunta e as evidências recuperadas são enviadas ao provedor de IA configurado.',
  'Identify strengths': 'Identificar pontos fortes', 'Improve documentation': 'Melhorar documentação',
  'Which backend engineering strengths are supported by my repositories?': 'Quais pontos fortes em engenharia backend são sustentados pelos meus repositórios?',
  'How can I make my portfolio documentation more useful?': 'Como posso tornar a documentação do meu portfólio mais útil?',
  'Analyze portfolio': 'Analisar portfólio', 'Analyzing…': 'Analisando…',
  'Select a developer and ask a question to begin.': 'Selecione um desenvolvedor e faça uma pergunta para começar.',
  'Advisor is unavailable in this environment': 'A análise está indisponível neste ambiente',
  'The complete analysis is enabled only in the local Quarkus development profile. Retrieval Explorer below uses public sources.': 'A análise completa está habilitada apenas no perfil local de desenvolvimento do Quarkus. O explorador de evidências utiliza fontes públicas.',
  'What evidence are you looking for?': 'Quais evidências você procura?',
  'How do these repositories use testing and database migrations?': 'Como esses repositórios utilizam testes e migrações de banco de dados?',
  'Uses the selected developer ID. Searches indexed public documents only; no model-generated answer.': 'Utiliza o ID selecionado. Busca somente documentos públicos indexados, sem resposta gerada por IA.',
  'Retrieval mode': 'Modo de busca', 'Search Evidence': 'Buscar evidências', 'Compare both modes': 'Comparar os dois modos',
  'FOCUSED returns up to 8 chunks ranked by similarity, without a minimum similarity threshold.': 'FOCUSED retorna até 8 trechos ordenados por similaridade, sem limite mínimo de similaridade.',
  'OVERVIEW returns up to 8 chunks, at most 2 per source, with cosine similarity ≥ 0.65.': 'OVERVIEW retorna até 8 trechos, no máximo 2 por fonte, com similaridade cosseno ≥ 0,65.',
  'Cosine similarity is a ranking measure, not a percentage of certainty. Compare modes with the same question and developer.': 'A similaridade cosseno é uma medida de ordenação, não uma porcentagem de certeza. Compare os modos com a mesma pergunta e o mesmo desenvolvedor.',
  'Ask a question to inspect public evidence.': 'Faça uma pergunta para explorar evidências públicas.',
  'Enter a positive developer ID within the supported range.': 'Informe um ID positivo de desenvolvedor dentro do intervalo permitido.',
  'Unable to reach the server. Check the connection and try again.': 'Não foi possível acessar o servidor. Verifique a conexão e tente novamente.',
  'The request was rejected. Check the developer ID and question.': 'A solicitação foi rejeitada. Verifique o ID e a pergunta.',
  'Advisor analysis is available only in the local development demo.': 'A análise está disponível apenas na demonstração local de desenvolvimento.',
  'The requested developer or resource was not found.': 'O desenvolvedor ou recurso solicitado não foi encontrado.',
  'The service is busy. Please wait before trying again.': 'O serviço está ocupado. Aguarde antes de tentar novamente.',
  'The server could not complete the request. Check that the developer exists and the backend is configured.': 'O servidor não conseguiu concluir a solicitação. Verifique se o desenvolvedor existe e se o backend está configurado.',
  'The server returned an unreadable response. Please try again.': 'O servidor retornou uma resposta ilegível. Tente novamente.',
  'Languages': 'Linguagens', 'Frameworks': 'Frameworks', 'Databases': 'Bancos de dados', 'Architecture': 'Arquitetura', 'Tools': 'Ferramentas', 'Other': 'Outras',
  'Loading developer profile…': 'Carregando perfil do desenvolvedor…', 'No developer profile was returned.': 'Nenhum perfil de desenvolvedor foi retornado.',
  'Unnamed developer': 'Desenvolvedor sem nome', 'No GitHub username declared.': 'Nenhum usuário do GitHub declarado.',
  'Declared base stack · not verified by repository evidence': 'Stack base declarada · não verificada por evidências dos repositórios',
  'No technical skills have been declared.': 'Nenhuma competência técnica foi declarada.',
  'Profile request timed out. Try refreshing.': 'O carregamento do perfil excedeu o tempo limite. Tente atualizar.',
  'Waiting for developer ID…': 'Aguardando o ID do desenvolvedor…', 'Enter a question before continuing.': 'Informe uma pergunta antes de continuar.',
  'Similarity unavailable': 'Similaridade indisponível', 'No analysis was returned. Try a different question.': 'Nenhuma análise foi retornada. Tente outra pergunta.',
  'Analysis': 'Análise', 'Demonstrated strengths': 'Pontos fortes demonstrados', 'No strengths were returned.': 'Nenhum ponto forte foi retornado.',
  'Recommendation': 'Recomendação', 'No recommendations were returned.': 'Nenhuma recomendação foi retornada.',
  'Sources': 'Fontes', 'Source without a public link': 'Fonte sem link público', 'No evidence sources were returned.': 'Nenhuma fonte de evidência foi retornada.',
  'Analysis complete. Review recommendations against the source evidence.': 'Análise concluída. Confira as recomendações com as evidências das fontes.',
  'Analyzing repository evidence…': 'Analisando evidências dos repositórios…', 'Analysis timed out. Please try again.': 'A análise excedeu o tempo limite. Tente novamente.',
  'Developer changed. Run a new analysis.': 'Desenvolvedor alterado. Execute uma nova análise.',
  'Inputs changed. Search again to refresh evidence.': 'Os parâmetros mudaram. Busque novamente para atualizar as evidências.',
  'The server returned an unreadable evidence response.': 'O servidor retornou uma resposta de evidências ilegível.',
  'No public evidence met the OVERVIEW similarity threshold (0.65). The developer may also have no indexed public documents.': 'Nenhuma evidência pública atingiu o limite de similaridade do OVERVIEW (0,65). O desenvolvedor também pode não ter documentos públicos indexados.',
  'No public evidence was found. This developer may have no indexed public documents.': 'Nenhuma evidência pública encontrada. Este desenvolvedor pode não ter documentos públicos indexados.',
  'Evidence chunk': 'Trecho de evidência', 'Search timed out. Please try again.': 'A busca excedeu o tempo limite. Tente novamente.',
  'Some evidence searches could not complete. Review the message for each mode.': 'Algumas buscas não foram concluídas. Confira a mensagem de cada modo.',
  'HIGH': 'ALTA', 'MEDIUM': 'MÉDIA', 'LOW': 'BAIXA'
};
Object.assign(portuguese, {
  'Data and privacy': 'Dados e privacidade', 'Quick questions': 'Perguntas sugeridas',
  'Ready to analyze': 'Pronto para analisar', 'Developer selection changed': 'Desenvolvedor alterado',
  'Analyzing evidence': 'Analisando evidências', 'Analysis ready': 'Análise disponível',
  'No analysis available': 'Nenhuma análise disponível', 'Analysis could not complete': 'Não foi possível concluir a análise',
  'Analysis progress': 'Progresso da análise', 'Recommendations': 'Recomendações',
  'Setup': 'Configuração', 'Developer setup': 'Configuração do desenvolvedor',
  'Setup form': 'Formulário de configuração', 'Ingestion progress': 'Progresso da ingestão',
  'Create a local profile and index public repositories. Private repositories are never requested by this flow.': 'Cadastre um perfil local e indexe os repositórios públicos. Este fluxo nunca solicita repositórios privados.',
  'Full name': 'Nome completo', 'GitHub username': 'Usuário do GitHub', 'Profile source': 'Origem do perfil',
  'Enter skills manually': 'Cadastrar competências manualmente', 'Import PDF resume': 'Importar currículo PDF',
  'PDF resume (up to 4 MiB)': 'Currículo PDF (até 4 MiB)',
  'The PDF is temporary. Extracted text is sent to the configured AI provider. Review the returned name and skills before saving. Use only a resume you are authorized to process.': 'O PDF é temporário. O texto extraído é enviado ao provedor de IA configurado. Revise o nome e as competências antes de salvar. Utilize somente um currículo que você tem autorização para processar.',
  'Extract for review': 'Extrair para revisão', 'Review declared skills': 'Revisar competências declaradas',
  'Edit names and categories, remove incorrect entries or add missing skills. Nothing is saved by importing a PDF.': 'Corrija nomes e categorias, remova entradas incorretas ou adicione competências. Importar um PDF não salva o perfil.',
  'Add skill': 'Adicionar competência', 'Remove': 'Remover', 'Skill name': 'Nome da competência', 'Skill category': 'Categoria da competência',
  'Save profile and index public repositories': 'Salvar perfil e indexar repositórios públicos',
  'Choose manual entry or PDF import to begin.': 'Escolha o cadastro manual ou a importação de PDF para começar.',
  'Public repository ingestion': 'Ingestão de repositórios públicos', 'No ingestion job has been started.': 'Nenhuma ingestão foi iniciada.',
  'Start public ingestion': 'Iniciar ingestão pública', 'Check progress': 'Consultar progresso',
  'Setup is available only on the local development server. Uploads, profile creation and ingestion are not exposed as public operations.': 'A configuração está disponível apenas no servidor local de desenvolvimento. Uploads, cadastro e ingestão não estão expostos como operações públicas.',
  'Enter a valid GitHub username (up to 30 characters).': 'Informe um usuário válido do GitHub (até 30 caracteres).',
  'A profile supports up to 100 skills.': 'Um perfil suporta até 100 competências.',
  'The server returned an unreadable ingestion response.': 'O servidor retornou uma resposta de ingestão ilegível.',
  'Repository progress': 'Progresso dos repositórios', 'Total repositories': 'Total de repositórios', 'Processed': 'Processados', 'Skipped': 'Ignorados', 'Failed': 'Falhas', 'Chunks': 'Trechos',
  'Current repository': 'Repositório atual', 'Next attempt': 'Próxima tentativa',
  'Ingestion complete. Profile, evidence and analysis tabs are ready.': 'Ingestão concluída. As abas de perfil, evidências e análise estão disponíveis.',
  'Ingestion failed. Check the local backend configuration before retrying.': 'A ingestão falhou. Verifique a configuração local do backend antes de tentar novamente.',
  'Ingestion is running. Progress comes from the backend job.': 'Ingestão em andamento. O progresso é informado pelo job do backend.',
  'Choose a PDF between 1 byte and 4 MiB.': 'Selecione um PDF entre 1 byte e 4 MiB.',
  'Extracting resume text. Nothing has been saved yet.': 'Extraindo o texto do currículo. Nada foi salvo ainda.',
  'The resume returned no valid profile data. Use manual entry or try another PDF.': 'O currículo não retornou dados de perfil válidos. Use o cadastro manual ou tente outro PDF.',
  'Review the extracted name and skills, then save the profile.': 'Revise o nome e as competências extraídos antes de salvar o perfil.',
  'Resume import timed out. Try again or use manual entry.': 'A importação excedeu o tempo limite. Tente novamente ou use o cadastro manual.',
  'Enter a full name with up to 60 characters.': 'Informe um nome completo com até 60 caracteres.',
  'Import and review the PDF before saving, or choose manual entry.': 'Importe e revise o PDF antes de salvar ou escolha o cadastro manual.',
  'Each skill needs a name (up to 40 characters) and a valid category.': 'Cada competência precisa de nome (até 40 caracteres) e categoria válida.',
  'Saving developer profile…': 'Salvando perfil do desenvolvedor…', 'The server did not return a developer ID.': 'O servidor não retornou o ID do desenvolvedor.',
  'Profile saved. Starting public repository ingestion.': 'Perfil salvo. Iniciando a ingestão de repositórios públicos.',
  'Operations are available only in the local development demo.': 'As operações estão disponíveis apenas na demonstração local de desenvolvimento.',
  'A profile with this username or an active ingestion job may already exist.': 'Já pode existir um perfil com esse usuário ou um job de ingestão ativo.',
  'The upload exceeds the supported size.': 'O upload excede o tamanho permitido.'
});

function translated(text, language) {
  if (language !== 'pt') return text;
  if (portuguese[text]) return portuguese[text];
  return text.replace(/^Cosine similarity (.+)$/, 'Similaridade cosseno $1')
    .replace(/^(\d+) chunks · (\d+) distinct repositories$/, '$1 trechos · $2 repositórios distintos')
    .replace(/^Searching public evidence for developer (.+)…$/, 'Buscando evidências públicas para o desenvolvedor $1…')
    .replace(/^Search complete · developer (.+) · public sources only$/, 'Busca concluída · desenvolvedor $1 · somente fontes públicas');
}


function translateUi(language) {
  // Synchronous, one browser task: no intermediate locale can be painted.
  // Skip unchanged text to preserve DOM nodes, focus, selection and live regions.
  document.documentElement.lang = language === 'pt' ? 'pt-BR' : 'en';
  document.querySelectorAll('[data-i18n]').forEach(node => {
    const key = node.dataset.i18n || node.textContent.trim();
    node.dataset.i18n = key;
    const value = translated(key, language);
    if (node.textContent !== value) node.textContent = value;
  });
  document.querySelectorAll('[data-i18n-placeholder]').forEach(node => {
    node.dataset.i18nPlaceholder ||= node.placeholder;
    const value = translated(node.dataset.i18nPlaceholder, language);
    if (node.placeholder !== value) node.placeholder = value;
  });
  document.querySelectorAll('[aria-label]').forEach(node => {
    node.dataset.originalLabel ||= node.getAttribute('aria-label');
    const value = translated(node.dataset.originalLabel, language);
    if (node.getAttribute('aria-label') !== value) node.setAttribute('aria-label', value);
  });
  const flag = document.getElementById('language-flag');
  const path = language === 'pt' ? '/flags/brazil.svg' : '/flags/united-kingdom.svg';
  if (flag && flag.getAttribute('src') !== path) flag.setAttribute('src', path);
  const toggle = document.getElementById('language-toggle');
  if (toggle) {
    toggle.setAttribute('aria-label', language === 'pt' ? 'Idioma: português. Mudar para inglês' : 'Language: English. Switch to Portuguese');
    toggle.title = language === 'pt' ? 'Mudar para inglês' : 'Switch to Portuguese';
  }
  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    const dark = document.documentElement.dataset.theme === 'dark';
    const key = dark ? 'Light theme' : 'Dark theme';
    themeToggle.dataset.i18n = key;
    const value = translated(key, language);
    if (themeToggle.textContent !== value) themeToggle.textContent = value;
    themeToggle.setAttribute('aria-label', translated(dark ? 'Switch to light theme' : 'Switch to dark theme', language));
  }
  delete document.documentElement.dataset.languagePending;
}
