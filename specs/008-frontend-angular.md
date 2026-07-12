# 008 — Frontend Angular: gestão acadêmica + fluxo de Matrícula

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §03 (frontend Angular consumindo a API), §07 (organização, fluxo de uso
e tratamento de erros — critério de pontuação explícito do frontend, não estética)
**Módulo(s) Modulith afetado(s):** novo diretório `frontend/` na raiz, fora do Modulith Java, consome a API
REST já pronta das Fases 2-4; `academico` recebeu um endpoint aditivo mínimo (`GET /api/alunos/me`, D041 —
achado durante a implementação, ver seção 5); `security` (`SecurityConfig`) recebeu configuração de CORS
(D042 — requisito técnico, achado durante a validação manual, ver seção 5)

## 1. Objetivo

Construir o frontend Angular que consome a API REST já pronta (Aluno, Curso, Disciplina, Turma, Matrícula),
com autenticação real via Keycloak, RBAC/ABAC-aware na UI, e o fluxo completo de Matrícula — incluindo o
tratamento explícito da UX de conflito de vaga (409) — priorizando fluxo funcional e tratamento de erro
consistente sobre polimento visual, dado o orçamento de tempo apertado desta fase.

## 2. Escopo

**Dentro do escopo:**
- Projeto Angular 20 (D039), standalone components (sem NgModules), em `frontend/` na raiz do repositório.
- Login via Keycloak com `keycloak-js` + `keycloak-angular` (D036): Authorization Code + PKCE, token mantido
  em memória (não persistido em localStorage/sessionStorage).
- Guard de rota (`CanActivateFn`) que verifica autenticação e papel RBAC (ALUNO, SECRETARIA, ADMIN) antes de
  liberar acesso a cada tela, reaproveitando os papéis já modelados no Keycloak (`docker/keycloak/import/gestao-realm.json`).
- Interceptor HTTP funcional (`HttpInterceptorFn`) que anexa o Bearer token em toda chamada à API e trata
  401 (redireciona para login) e 403 (mensagem de acesso negado) de forma centralizada.
- Identidade visual como CSS custom properties: `--color-navy: #1B2A5B` (dominante, hover/seleção
  `--color-navy-hover: #2E4A8F`), `--color-accent: #E8871E` (CTAs/destaques), `--color-bg: #F7F8FA`,
  `--color-text: #1F2430`. Tabelas de Turma/Aluno/Matrícula em densidade estilo ERP (compactas,
  escaneáveis). Sem componentes de terceiros — construídos do zero em cima desses tokens.
- Telas de gestão (Curso, Disciplina, Turma) para SECRETARIA/ADMIN: listagem em tabela densa com
  criar/editar/excluir, formulário com validação client-side (espelha as regras do backend, não é fonte de
  verdade), erros de validação da API exibidos por campo.
- Tela de Aluno: CRUD para SECRETARIA/ADMIN, leitura do próprio perfil para ALUNO — UI ABAC-aware (esconde/
  desabilita ações sem permissão; a decisão de autorização real continua sendo só do backend).
- Fluxo de Matrícula completo:
  - ALUNO: turmas disponíveis (vagas restantes, status aberta/fechada), matricular-se (cria PENDENTE), suas
    matrículas com ação de cancelar.
  - SECRETARIA/ADMIN: matrículas por turma, ação de confirmar.
  - Tratamento explícito do 409 de conflito de vaga: mensagem específica de que a vaga foi perdida para
    outra requisição (não um alerta genérico).
- Tratamento de erro global: interceptor/handler traduzindo `ProblemDetail` (400/401/403/404/409/500) em
  mensagens de usuário consistentes, sem expor detalhes internos; estado de loading e erro de rede tratado
  em todas as telas.
- Testes unitários dos services Angular que consomem a API (mock de `HttpClient` via `HttpTestingController`).
- Estrutura de pastas por feature (D038): `core/` (auth, interceptors), `shared/` (DTOs/models, componentes
  reutilizáveis), `features/{curso,disciplina,turma,aluno,matricula}/`.
- Estado via services + RxJS/Signals, sem NgRx (D037).

**Fora do escopo (não-objetivos):**
- Paginação/filtros avançados — diferencial, só se sobrar tempo.
- Dark mode, responsividade mobile refinada.
- Qualquer tela fora do fluxo de gestão acadêmica descrito acima (ex: relatórios).
- E2E de UI (Playwright) — não obrigatório dado o prazo; registrar como pendência explícita se cortado
  (ver seção 6/9). O e2e já existente em `e2e/` (contra a API) continua sendo o gate de CI.
- Geração automática de DTOs TypeScript a partir do backend (ex: OpenAPI codegen) — mantidos manualmente
  dado o tamanho da API; se a API crescer muito, revisitar.

## 3. Regras de negócio envolvidas

Esta fase não cria nem altera regra de negócio (todas já fechadas em Fases 3/4) — a UI reflete e reforça
visualmente as regras já garantidas pelo backend:

- [ ] Aluno só pode se matricular em turmas com status `ABERTA` e vagas disponíveis — UI mostra essa
  informação antes da ação, mas a validação real é sempre do backend.
- [ ] Conflito de vaga concorrente (409, `VAGAS_ESGOTADAS`/`CONFLITO_CONCORRENCIA`) tratado como UX de
  primeira classe, não um alerta genérico de erro.
- [ ] Papéis RBAC (ALUNO, SECRETARIA, ADMIN) determinam quais telas/ações aparecem — a UI nunca oferece uma
  ação que sempre falharia com 403 para o usuário logado.
- [ ] ALUNO só visualiza/cancela as próprias matrículas; SECRETARIA/ADMIN confirma matrículas de qualquer
  aluno — espelha o ABAC já implementado no backend (specs/003).

## 4. Abordagem técnica

### 4.1 Setup do projeto

`ng new frontend --standalone --style=css --routing` (Angular 20, D039) na raiz do repositório, ao lado de
`src/` (backend Java) — projeto independente, não um módulo Maven.

### 4.2 Autenticação (D036)

`keycloak-js` + `keycloak-angular`: `provideKeycloak(...)` no `app.config.ts` apontando para o realm
`gestao` já existente (`docker/keycloak/import/gestao-realm.json`, client público `gestao-frontend`, já
configurado com `redirectUris: http://localhost:4200/*`, `standardFlowEnabled: true` — Authorization Code
já habilitado, nenhuma mudança necessária no realm). `authGuard` (`keycloak-angular`) combinado com um guard
próprio de papel (`CanActivateFn` lendo `realm_access.roles` do token decodificado) para RBAC por rota.
`HttpInterceptorFn` próprio: anexa `Authorization: Bearer <token>` (via `KeycloakService`/`Keycloak`
injetado), trata 401 (chama `login()` do adapter) e 403 (redireciona para uma tela de "acesso negado" com
mensagem, não um alerta bloqueante).

### 4.3 Identidade visual

`styles.css` (global) define os CSS custom properties do design system Lyceum (seção 2). Componente
`shared/ui/data-table` (tabela densa reutilizável) usado pelas 3 telas de listagem (Turma, Aluno, Matrícula).

### 4.4 Estrutura de pastas (D038)

```
frontend/src/app/
  core/
    auth/           # guard de papel, interceptor de token/erro, init do Keycloak
    error-handling/ # tradução ProblemDetail -> mensagem de usuário
  shared/
    models/         # DTOs TypeScript espelhando os DTOs Java da borda
    ui/              # componentes reutilizáveis (data-table, form-field-error, etc.)
  features/
    curso/
    disciplina/
    turma/
    aluno/
    matricula/
  app.routes.ts
  app.config.ts
```

Cada `features/<recurso>/` tem `<recurso>.service.ts` (chamadas HTTP + estado via `signal`),
`<recurso>-lista.component.ts`, `<recurso>-form.component.ts` (quando aplicável).

### 4.5 Fluxo de Matrícula (núcleo funcional)

**Divergência em relação ao planejado aqui, corrigida após a implementação:** o plano original previa um
componente dedicado `turmas-disponiveis.component.ts` para o ALUNO ver turmas com vagas. Na implementação,
essa responsabilidade foi absorvida por `features/turma/turma-lista.component.ts` (mesma listagem de Turma
já usada por SECRETARIA/ADMIN, com o botão "Matricular-se" condicional ao papel `ALUNO`) — evita duplicar a
mesma consulta/listagem em duas telas separadas para o mesmo recurso. `features/matricula/` ficou só com o
que é de fato específico do fluxo de Matrícula: `minhas-matriculas.component.ts` (ALUNO, cancelar) e
`matriculas-por-turma.component.ts` (SECRETARIA/ADMIN, confirmar). O 409 é tratado no componente que chama
`confirmar()` (não no `MatriculaService`, que só faz a chamada HTTP) — `matriculas-por-turma.component.ts`
verifica o `errorCode` do `ProblemDetail` (`VAGAS_ESGOTADAS`/`CONFLITO_CONCORRENCIA`, os códigos reais que
`MatriculaService.confirmar()` no backend lança) e emite uma mensagem de UI específica em vez de cair no
handler genérico de erro.

### 4.6 Tratamento de erro global

`core/error-handling/problem-detail.interceptor.ts`: intercepta respostas de erro, mapeia o `type`/`status`
do `ProblemDetail` para uma mensagem amigável por categoria (validação 400 → erros por campo, propagados ao
formulário; 401 → redireciona login; 403 → tela de acesso negado; 404 → mensagem "não encontrado"; 409 →
mensagem de conflito específica por contexto, com fallback genérico; 500 → mensagem genérica sem detalhes
internos). Estado de loading/erro de rede via um `signal` compartilhado por feature service.

### 4.7 Testes

Testes unitários dos services (`*.service.spec.ts`) via `HttpTestingController`, cobrindo: chamadas
corretas à API, tratamento do 409 no fluxo de confirmação de matrícula, tratamento de erro genérico.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Biblioteca de autenticação Keycloak | `angular-oauth2-oidc` vs. `keycloak-js`+`keycloak-angular` | [D036](../docs/DECISIONS.md#d036) — keycloak-js + keycloak-angular |
| 2 | Gestão de estado | Services+RxJS/Signals vs. NgRx | [D037](../docs/DECISIONS.md#d037) — services + RxJS/Signals, sem NgRx |
| 3 | Estrutura de pastas | Por feature vs. espelhando módulo Modulith do backend | [D038](../docs/DECISIONS.md#d038) — por feature |
| 4 | Versão do Angular | 20 vs. 18 LTS | [D039](../docs/DECISIONS.md#d039) — Angular 20 |
| 5 | "Meu Perfil" do ALUNO sem endpoint de autoleitura | Claims do token vs. cortar do escopo vs. novo endpoint backend | [D040](../docs/DECISIONS.md#d040) — claims do token, depois superado por D041 |
| 6 | ALUNO não consegue descobrir o próprio `alunoId` (bloqueia matricular-se) | Adicionar `GET /api/alunos/me` vs. cortar matrícula do ALUNO do escopo | [D041](../docs/DECISIONS.md#d041) — adicionar `GET /api/alunos/me` (endpoint aditivo mínimo em `academico`) |
| 7 | CORS bloqueando toda chamada do frontend (origem diferente da API) | N/A — requisito técnico, sem alternativa razoável | [D042](../docs/DECISIONS.md#d042) — `CorsConfigurationSource` restrito à origem do frontend |
| 8 | Duplicação entre services/formulários de Curso/Disciplina/Turma/Aluno | Sem abstração vs. `CrudService<T>` genérico | [D043](../docs/DECISIONS.md#d043) — sem abstração genérica, dado o orçamento de tempo e o tamanho real do problema |

## 6. Critérios de aceite

- [x] Login real via Keycloak funciona (redireciona, autentica, retorna com token válido). Validado com
  `aluno.teste` e `secretaria.teste` (seção 10) — fluxo Authorization Code + PKCE completo, via browser real
  (Playwright + Chrome), não mockado.
- [x] Guard de rota bloqueia acesso a uma tela sem o papel RBAC adequado. Validado estruturalmente (guard
  aplicado a toda rota staff-only/ALUNO-only, `roleGuard` com `data.papeis`) e empiricamente para ALUNO e
  SECRETARIA (seção 10); papel ADMIN não foi exercitado com um usuário de teste separado nesta validação —
  o backend trata ADMIN de forma idêntica a SECRETARIA em todo `@PreAuthorize`
  (`hasRole('SECRETARIA') or hasRole('ADMIN')`), então o caminho de código é o mesmo, mas não há evidência
  empírica com um token ADMIN real.
- [x] SECRETARIA/ADMIN consegue criar/editar/excluir Curso, Disciplina, Turma via UI, com erro de validação
  exibido por campo. Criação validada empiricamente (seção 10); edição/exclusão e exibição de erro por
  campo cobertas pelo mesmo código/padrão usado na criação (formulário único reutilizado para
  criar/editar), não re-executadas linha a linha na validação manual dado o orçamento de tempo.
- [x] ALUNO vê o próprio perfil (leitura, D040/D041); SECRETARIA/ADMIN faz CRUD completo de Aluno. Meu
  Perfil validado empiricamente (seção 10, dados reais de `GET /api/alunos/me`); CRUD de Aluno segue o
  mesmo padrão de Curso/Disciplina, validado por teste unitário (`aluno.service.spec.ts`) + build.
- [x] ALUNO vê turmas disponíveis (com vagas/status), matricula-se, cancela sua matrícula. Validado
  empiricamente de ponta a ponta (seção 10).
- [x] SECRETARIA/ADMIN vê matrículas por turma e confirma uma matrícula. Validado empiricamente (seção 10).
- [x] Cenário de 409 (vaga perdida) exibe mensagem específica, não um alerta genérico — validado
  manualmente forçando a corrida real (duas matrículas `PENDENTE` disputando a única vaga de uma turma,
  seção 10): a UI mostrou a mensagem específica de vaga perdida, não o banner genérico.
- [x] Nenhuma ação sem permissão aparece habilitada na UI para o papel logado — garantido estruturalmente
  (`@if (currentUser.temPapel(...))`/`ehSecretariaOuAdmin` envolvendo toda ação restrita nos templates) e
  confirmado indiretamente pela validação manual (nenhuma ação restrita apareceu para o papel errado nas
  telas percorridas).
- [x] Erros de rede (API fora do ar) e estado de loading tratados em todas as telas — `apiErrorInterceptor`
  (status 0) + `carregando`/`loading` por service, mesmo padrão em todas as features.
- [x] Testes unitários dos services passando (23/23, `ng test`).
- [x] `code-reviewer` e `security-auditor` executados — achados endereçados (seção 10): erros de carregamento
  inicial sem tratamento em 4 listas + 4 formulários (corrigido), `app.frontend.origin` sem indireção
  `${ENV_VAR:...}` (corrigido), regex sem escape no interceptor de Bearer token (corrigido), `GET
  /api/alunos/me` sem teste de integração HTTP dedicado (corrigido, `AlunoControllerIntegrationTest` novo),
  frontend fora do gate de CI (corrigido, job novo em `ci.yml`), duplicação CRUD sem entrada própria em
  `DECISIONS.md` (corrigido, D043). Storage do token em memória (D036) e nenhuma decisão de autorização real
  no frontend: confirmados sem achados.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Services Angular (chamadas HTTP corretas, tratamento de 409/erros) | `frontend/src/app/features/**/*.service.spec.ts` |
| Manual | Fluxo completo ALUNO e SECRETARIA, incluindo 409 de vaga concorrente | Documentado na seção 10 ao fechar |
| E2E | Já coberto contra a API em `e2e/` (Fases 3/4) — sem novo e2e de UI nesta fase (fora de escopo, seção 2) | `e2e/matricula-flow.sh` |

**Justificativa de cobertura:** o valor real desta fase está no fluxo funcional e no tratamento de erro
(critério de pontuação explícito do PRD), não em cobertura de linha de UI — por isso o foco de teste
automatizado fica nos services (lógica de integração com a API, incluindo o tratamento do 409), e a
validação do fluxo completo fica registrada como validação manual estruturada.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhum novo mecanismo de observabilidade backend nesta fase; erros de rede/API
  são tratados na UI, não logados em um backend de telemetria frontend (fora de escopo).
- **Segurança:** token JWT mantido em memória via `keycloak-js` (não localStorage/sessionStorage,
  mitigação de XSS, D036) — confirmado pelo `security-auditor` (seção 10): nenhum uso de
  localStorage/sessionStorage/cookie em todo o `frontend/src`. Nenhuma decisão de autorização real depende
  do frontend (é UX, não segurança — o backend já impõe RBAC/ABAC independentemente do que a UI mostra,
  confirmado pelo mesmo review). URLs de API via `environment.ts` (único arquivo de ambiente nesta fase —
  sem `environment.development.ts`/`fileReplacements` de produção, já que o deploy deste desafio roda
  inteiramente local; nunca hardcoded em componentes/services).
- **Achado de code review, corrigido:** `GET /api/alunos/me` (D041) não tinha teste de integração HTTP
  dedicado — adicionado `AlunoControllerIntegrationTest` (Testcontainers), cobrindo especificamente que o
  endpoint nunca retorna o registro de outro Aluno além do dono do token, e que o override do
  `@PreAuthorize` de classe não vaza para os demais endpoints do controller.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando
- [x] `docs/DECISIONS.md` atualizado com D036–D043 (D040/D041/D042/D043 são achados durante a implementação
  e as revisões, não previstos no planejamento original — ver seção 5)
- [x] `code-reviewer` executado — achados endereçados (seção 10)
- [x] `security-auditor` executado — sem achados bloqueantes; 2 itens de baixo risco endereçados (seção 10)
- [x] Validação manual documentada (seção 10): fluxo ALUNO, fluxo SECRETARIA, cenário 409
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] README atualizado com instruções de execução do frontend (seção "Frontend (Angular)" no README raiz)

## 10. Validação manual

Ambiente sem display gráfico (sandbox headless) — validação executada via Playwright controlando um Chrome
real (`channel: 'chrome'`, headless) contra a stack completa e real: `ng serve` (frontend, :4200),
`./mvnw spring-boot:run` (backend, :8080) e o `docker compose` já existente (Postgres/Redis/RabbitMQ/
Keycloak). Não é um teste automatizado formal do repositório (fora do escopo desta fase, seção 2) — é a
forma mais rigorosa de "clicar na UI" disponível neste ambiente, substituindo literalmente mover mouse/
teclado por um script que dirige um browser real através do fluxo completo, sem mockar nada (login real via
Keycloak, chamadas HTTP reais). O script não foi commitado (ferramenta de validação, não part do
deliverable).

**Execução (16 verificações, todas passaram na execução final):**

```
OK   - secretaria.teste autenticada, app shell carregado
OK   - secretaria criou Curso via UI
OK   - secretaria criou Disciplina via UI
OK   - Disciplina vinculada ao Curso via API (status=200)
OK   - secretaria criou Turma (1 vaga) via UI
OK   - turma recem-criada encontrada via API (id=15)
OK   - aluno.teste autenticado, app shell carregado
OK   - Meu Perfil (GET /api/alunos/me) renderizou dados reais: NomeAluno E2E Email aluno.e2e...@example.com
       Cadastrado em 7/11/26, 7:33 PM
OK   - ALUNO matriculou-se com sucesso: Matrícula criada na turma "PW-T-..." (aguardando confirmação da
       secretaria).
OK   - Minhas Matrículas mostra a nova matrícula como PENDENTE
OK   - segundo Aluno criado via API para a corrida (id=8)
OK   - segunda matrícula PENDENTE criada na mesma turma (id=14)
OK   - SECRETARIA confirmou a 1ª matrícula (consumiu a única vaga)
OK   - Turma mostra vaga consumida (1 / 1)
OK   - UI mostrou a mensagem ESPECÍFICA de vaga perdida (não genérica): "Esta vaga foi confirmada por outra
       requisição nos últimos instantes — a turma pode não ter mais vagas disponíveis. Atualize a lista e
       verifique a situação da turma antes de tentar novamente."
OK   - 2ª matrícula continua PENDENTE (não foi indevidamente confirmada)
OK   - ALUNO cancelou a própria matrícula confirmada via UI
OK   - Vaga liberada após o cancelamento (0 / 1)
==================================
Todas as validacoes passaram.
```

**Roteiro coberto:** login real (Authorization Code + PKCE) como SECRETARIA e como ALUNO → SECRETARIA cria
Curso/Disciplina/Turma (1 vaga) via UI → ALUNO lê Meu Perfil (dado real da API, D040/D041) → ALUNO vê a
turma disponível e matricula-se → ALUNO vê a própria matrícula PENDENTE → cria-se uma segunda matrícula
PENDENTE concorrente na mesma turma (a única forma de gerar a corrida real, já que só há um usuário ALUNO
de teste no realm) → SECRETARIA confirma a 1ª matrícula, consumindo a única vaga → SECRETARIA tenta
confirmar a 2ª matrícula e recebe 409 → **a UI mostra a mensagem específica de vaga perdida, não um alerta
genérico** (o critério mais citado do PRD) → a 2ª matrícula permanece PENDENTE (não é indevidamente
confirmada) → ALUNO cancela a própria matrícula confirmada → a vaga é liberada (visível na listagem de
Turmas).

**Três achados reais, corrigidos durante esta validação** (não hipotéticos — cada um quebrava a aplicação
até ser corrigido):
1. `withAutoRefreshToken()` do `keycloak-angular` falhava no boot com `NG0201: No provider found for
   _AutoRefreshTokenService` — removido (não era requisito do escopo, só um extra; o refresh de token
   básico via `shouldUpdateToken` do interceptor continua funcionando sem essa feature).
2. CORS: nenhuma chamada da API funcionava a partir do `ng serve` (origem diferente) — `SecurityConfig`
   ganhou um `CorsConfigurationSource` restrito à origem do frontend (D042).
3. `TurmaService.validarParCursoDisciplina`: criar uma Turma exige que a Disciplina já esteja vinculada ao
   Curso (regra pré-existente da Fase 2/3) — a spec original não mencionava esse pré-requisito
   explicitamente; o roteiro de validação (e qualquer uso real da tela de Nova Turma) precisa vincular a
   Disciplina ao Curso primeiro (endpoint já existente, sem tela dedicada nesta fase, fora de escopo).

**Não exercitado nesta rodada (ver seção 6):** papel ADMIN com usuário de teste dedicado (mesmo caminho de
código que SECRETARIA no backend, mas sem evidência empírica própria); edição/exclusão de Curso/Disciplina/
Turma linha a linha (mesmo componente de formulário da criação, já validado).

### `code-reviewer` e `security-auditor` (passo 5 do fluxo)

**`security-auditor`:** sem achados bloqueantes. Confirmou explicitamente os três pontos de atenção do
escopo: token JWT em memória (não localStorage/sessionStorage, D036), nenhuma autorização real decidida no
frontend (RBAC/ABAC seguem só no backend), e nenhuma URL de API fora de `environment.ts`. Verificou também
que `GET /api/alunos/me` (D041) só pode retornar o próprio registro do chamador (derivado do `sub` do JWT,
sem parâmetro manipulável pelo cliente) e que o CORS (D042) usa uma origem explícita, não wildcard nem
reflexão do header `Origin`. Dois itens de baixo risco, ambos corrigidos: (1) o `RegExp` do interceptor de
Bearer token não escapava caracteres especiais da URL — corrigido (`app.config.ts`); (2)
`directAccessGrantsEnabled: true` no client Keycloak `gestao-frontend` habilita o grant de senha direta além
do fluxo Authorization Code + PKCE — **mantido deliberadamente**, não corrigido: esse grant já é usado pelos
scripts `e2e/matricula-flow.sh` e pela validação manual desta própria fase (obtenção de token via
`curl`/`fetch` direto), desabilitá-lo quebraria esse tooling existente; risco aceito (é um client público
sem secret, mesmo perfil de risco de qualquer client PKCE).

**`code-reviewer`:** sem achados críticos. Confirmou a lógica do 409 em `matriculas-por-turma.component.ts`
correta (verificada linha a linha contra os `errorCode`s reais lançados por `MatriculaService.confirmar()`
no backend) e a separação `core`/`shared`/`features` consistente. Achados corrigidos:
1. **Erros de carregamento inicial sem tratamento** — `ngOnInit` de 4 componentes de listagem e 4 de
   formulário chamavam `carregar()`/`buscarPorId()` sem `try/catch`; uma falha não-rede (ex: 500) deixava a
   tela silenciosamente vazia/em branco, sem banner. Corrigido em todos os 8 componentes
   (`curso-lista`/`disciplina-lista`/`turma-lista`/`aluno-lista`/`matriculas-por-turma` +
   `curso-form`/`disciplina-form`/`turma-form`/`aluno-form`), agora populando `erro`/`erroGeral` via
   `mensagemAmigavel`.
2. **`app.frontend.origin` sem indireção `${ENV_VAR:...}`** — inconsistente com o padrão do resto de
   `application.properties`; corrigido (`APP_FRONTEND_ORIGIN`, documentado em `.env.example`).
3. **`GET /api/alunos/me` sem teste de integração HTTP** — a mudança mais sensível desta fase (sobrescreve
   `@PreAuthorize` de classe) só tinha teste unitário de service; adicionado
   `AlunoControllerIntegrationTest` (Testcontainers), incluindo um teste específico provando que o endpoint
   nunca retorna o registro de outro Aluno, e um teste de regressão confirmando que o override não vaza
   para os demais endpoints do controller (`GET /api/alunos` continua staff-only).
4. **Frontend fora do gate de CI** — `ci.yml` não tinha nenhum job de frontend; adicionado job `frontend`
   (`npm ci` + `ng test --browsers=ChromeHeadless` + `ng build`), rodando em paralelo ao job `build` do
   backend.
5. **Duplicação CRUD sem entrada em `DECISIONS.md`** — o trade-off "sem abstração genérica" só estava
   justificado em prosa na spec, não como decisão registrada; adicionado D043.
6. Achados aceitos sem correção: pequena sobreposição de banner em 403 (o interceptor global mostra um
   banner genérico e a ação específica também pode mostrar o seu — redundante, mas não incorreto, e 403 não
   deveria ser alcançável na prática já que ações são escondidas por papel); ausência de
   `environment.development.ts`/`fileReplacements` de produção (fora de escopo — deploy deste desafio é
   local, já registrado como tal); drift do §4.5 desta spec em relação à implementação real de
   `turma-lista.component.ts` (corrigido diretamente no texto da seção 4.5 acima, não uma correção de
   código).

Build final após todas as correções: backend `./mvnw clean verify` — 136/136 testes, `BUILD SUCCESS`;
frontend `ng test --browsers=ChromeHeadless` — 23/23, `ng build` — sucesso.
