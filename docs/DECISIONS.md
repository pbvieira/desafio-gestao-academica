# Log de Decisões Técnicas

Este documento existe para um propósito específico: **na entrevista técnica, mostrar exatamente quais
decisões foram deliberadas por mim (Pablo) e quais foram default/sugestão da IA aceita sem alteração.**
Não é um changelog de código — é um changelog de raciocínio.

Gerenciado pela skill `.claude/skills/decision-log/SKILL.md`. Toda decisão técnica não-trivial (mais de uma
alternativa razoável existia) tomada durante o desenvolvimento entra aqui, na ordem em que foi tomada.

## Como ler este documento

Cada entrada tem uma **Origem**, que é o dado mais importante para a entrevista:

| Origem | Significado |
|---|---|
| 🧑 **Decisão do Pablo** | Eu escolhi ativamente entre alternativas, possivelmente contra a sugestão inicial da IA |
| 🤝 **Sugestão da IA, revisada por mim** | A IA propôs, eu ajustei/critiquei antes de aceitar |
| 🤖 **Default da IA, aceito sem alteração** | A IA decidiu (por ser a prática comum) e eu concordei sem mudar nada |

## Índice

<!-- Atualize este índice a cada nova entrada -->
- [D001 — Monólito modular (Spring Modulith) em vez de microsserviços desde o início](#d001)
- [D002 — RabbitMQ em vez de Kafka para mensageria de domínio](#d002)
- [D003 — Aplicação entra no Docker Compose só numa fase posterior](#d003)
- [D004 — Isolamento do banco de dados do Keycloak](#d004)
- [D005 — Schema Postgres único (`public`) para todos os módulos](#d005)
- [D006 — Mapeamento Keycloak ↔ Aluno adiado para a Fase 2](#d006)
- [D007 — Grafana Loki + Promtail como backend de log centralizado](#d007)
- [D008 — Stack de observabilidade atrás de um Docker Compose profile opcional](#d008)
- [D009 — Dashboard básico (JVM/HTTP) provisionado por padrão no Grafana](#d009)
- [D010 — `oauth2-resource-server` em vez de `oauth2-client` para validar tokens](#d010)
- [D011 — Papéis RBAC: ALUNO, SECRETARIA, ADMIN](#d011)
- [D012 — `PermissionEvaluator` customizado como mecanismo de ABAC](#d012)
- [D013 — Client Keycloak confidential dedicado para o backend, já nesta fase](#d013)
- [D014 — Exclusões do gate de cobertura JaCoCo](#d014)
- [D015 — Pasta única `db/migration` para todos os módulos](#d015)
- [D016 — `type` (URI/URN) + propriedade `errorCode` no ProblemDetail](#d016)
- [D017 — Ponte de logs via arquivo (não appender direto) para o Promtail alcançar a app](#d017)
- [D018 — Mapeamento DTO↔entidade manual, sem MapStruct](#d018)
- [D019 — Soft delete (campo `ativo`) para Aluno/Curso/Disciplina/Turma](#d019)
- [D020 — Disciplina compartilhada entre Cursos (N:N), Turma com FK direto para Curso e Disciplina](#d020)
- [D021 — Leitura de Curso/Disciplina/Turma aberta a qualquer autenticado](#d021)
- [D022 — Modelagem menor: status de Turma, unicidade, vínculo Aluno↔Keycloak](#d022)
- [D023 — Correções decorrentes do code-reviewer/security-auditor da spec 005](#d023)
- [D024 — Mecanismo de proteção de vaga: UPDATE condicional atômico + `@Version`](#d024)
- [D025 — Contador de vagas ocupadas: campo incremental, não `COUNT` calculado](#d025)
- [D026 — Matrícula cancelada permite nova matrícula na mesma turma](#d026)
- [D027 — Confirmação de matrícula restrita a SECRETARIA/ADMIN](#d027)
- [D028 — Máquina de estados de Matrícula e resposta a conflito de vaga](#d028)
- [D029 — Sequenciamento de mensageria: eventos internos via Spring Modulith nesta fase, RabbitMQ na Fase 4](#d029)
- [D030 — Decisões menores agrupadas: extensão do ABAC, vínculo Aluno↔Keycloak, RBAC de listagem por turma, ferramenta de E2E](#d030)
- [D031 — Correções decorrentes do code-reviewer/security-auditor da spec 006](#d031)
- [D032 — Topic exchange para eventos de Matrícula externalizados](#d032)
- [D033 — Retry: 3 tentativas, TTL fixo de 10s, DLQ nativa do RabbitMQ](#d033)
- [D034 — Propagação de trace ID via suporte nativo do Spring Boot/AMQP (Observation)](#d034)
- [D035 — Decisões menores agrupadas: eventId para idempotência, topologia no pacote `config`, observabilidade da DLQ](#d035)
- [D036 — Biblioteca de autenticação Keycloak no frontend](#d036)
- [D037 — Gestão de estado no frontend: services + RxJS/Signals, sem NgRx](#d037)
- [D038 — Estrutura de pastas do frontend por feature](#d038)
- [D039 — Versão do Angular: 20](#d039)
- [D040 — "Meu Perfil" do ALUNO via claims do token (superada por D041)](#d040)
- [D041 — Endpoint `GET /api/alunos/me` para o ALUNO descobrir o próprio `alunoId`](#d041)
- [D042 — CORS restrito à origem do frontend](#d042)
- [D043 — Sem abstração `CrudService<T>` genérica no frontend](#d043)
- [D044 — Direção visual "Institucional acadêmico" para o redesign do frontend](#d044)
- [D045 — Escopo e integração técnica da administração de usuários/papéis](#d045)
- [D046 — Decisões menores agrupadas: módulo `administracao`, seção Administração da sidebar adiada, service account do `gestao-backend`](#d046)
- [D047 — Decisões do tema Keycloakify para o login (escopo, entrega, localização, estratégia de reskin)](#d047)
- [D048 — `AdministracaoUsuarioService.listarUsuarios()` resolve papel via membros de role (O(3)), não N+1 por usuário](#d048)
- [D049 — Ferramental da prova e2e de concorrência: Playwright isolado em `e2e/playwright/`, repetição via `--repeat-each`](#d049)
- [D050 — Teardown do e2e de concorrência: curso/disciplina fixos reaproveitados (teardown completo é impossível hoje)](#d050)
- [D051 — Código de comparação com lock pessimista isolado em `src/test/java`, nunca em produção](#d051)
- [D052 — Métrica `matricula.vaga.conflito` com tag `motivo`, primeira métrica com tag do projeto](#d052)
- [D053 — Decisão final: mantém UPDATE atômico condicional (D024), lock pessimista não substitui](#d053)
- [D054 — Captura de evidências visuais via Playwright com browser real, sessão única e descartável](#d054)
- [D055 — `docs/OBSERVABILIDADE.md` dedicado, README mantém só resumo + link](#d055)
- [D056 — Diagrama de módulos via `Documenter` do Spring Modulith, comitado como `.puml`; fluxos narrativos em Mermaid](#d056)

---

## Template para novas entradas

```markdown
## DNNN — <Título curto da decisão>

**Data:** AAAA-MM-DD
**Origem:** 🧑 / 🤝 / 🤖
**Spec relacionada:** specs/NNN-nome.md
**Contexto:** <por que essa decisão precisou ser tomada — que requisito ou problema a motivou>
**Alternativas consideradas:**
- Alternativa A — <prós/contras>
- Alternativa B — <prós/contras>
**Decisão:** <o que foi escolhido>
**Justificativa:** <por que essa e não as outras>
**Trade-offs aceitos:** <o que se perde ao escolher isso>
**Riscos conhecidos / o que revisitar se o contexto mudar:** <ex: "se o volume de matrículas simultâneas
crescer muito, revisitar a estratégia de lock">
```

---

<a id="d001"></a>
## D001 — Monólito modular (Spring Modulith) em vez de microsserviços desde o início

**Data:** 2026-07-10
**Origem:** 🤝 Sugestão da IA, revisada por mim
**Spec relacionada:** N/A (decisão de fundação, anterior à primeira spec)
**Contexto:** O PRD pede "solução modular ou baseada em mais de um serviço" e "separação clara entre
contexto acadêmico e contexto de notificações/auditoria/relatórios", mas não exige microsserviços
literais — exige desacoplamento real e demonstrável.

**Alternativas consideradas:**
- **Microsserviços desde o início** (2+ deploys separados, comunicação via broker) — atende ao requisito de
  forma mais "óbvia" para quem lê o README, mas para um desafio de 7 dias corridos introduz custo de
  infraestrutura (service discovery, contratos entre serviços, deploy duplicado) desproporcional ao ganho
  real de desacoplamento nesse escopo.
- **Camadas simples sem separação de módulo** — mais rápido, mas não atende ao requisito explícito do PRD
  de separação de contextos, e é justamente o padrão que a seção "Critérios críticos de avaliação" do PRD
  pune ("separação em serviços ou módulos é artificial e sem ganho arquitetural" — nesse caso nem existiria).
- **Monólito modular com Spring Modulith** — desacoplamento real e verificável em tempo de teste
  (`ApplicationModules.verify()`), publicação de eventos de domínio entre módulos, caminho de evolução claro
  para extrair um módulo como serviço separado no futuro (o PRD pergunta isso explicitamente na entrevista:
  "como você separaria esse módulo em outro serviço?").

**Decisão:** Monólito modular com Spring Modulith, dois módulos top-level (`academico` e um módulo
secundário de notificação/auditoria), comunicação entre eles via publicação de eventos de domínio.

**Justificativa:** Maximiza o desacoplamento demonstrável dentro do prazo de 7 dias, e dá uma resposta
concreta e defensável para a pergunta de entrevista sobre evolução para múltiplos serviços — em vez de
implementar microsserviços de forma apressada e superficial.

**Trade-offs aceitos:** Não há isolamento de deploy/processo entre os módulos — se o avaliador entender que
"mais de um serviço" significa estritamente múltiplos processos, essa decisão precisa ser bem defendida
na entrevista com a justificativa acima.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o feedback da triagem indicar que a banca
esperava múltiplos deploys de fato, o próximo passo natural é extrair o módulo secundário como serviço
consumidor via o broker externo (RabbitMQ/Kafka) já exigido pelo PRD — a mensageria real já presente na
solução reduz bastante esse custo de extração futura.

---

<a id="d002"></a>
## D002 — RabbitMQ em vez de Kafka para mensageria de domínio

**Data:** 2026-07-10
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/001-infraestrutura-base.md
**Contexto:** O PRD exige comunicação assíncrona real entre o contexto acadêmico e o contexto secundário
(notificação/auditoria), com publicação e consumo de eventos de domínio (`MatriculaCriada`,
`MatriculaConfirmada`, `MatriculaCancelada`).

**Alternativas consideradas:**
- **Kafka** — log distribuído, forte em replay e alto volume, mas operação (Zookeeper/KRaft, tópicos,
  partições) desproporcional ao volume e ao prazo de 7 dias corridos deste desafio.
- **RabbitMQ** — modelo de filas/AMQP mais simples de operar, configurar e explicar em entrevista, com
  management UI pronta para inspecionar filas/mensagens durante o desenvolvimento e a demonstração.

**Decisão:** RabbitMQ.

**Justificativa:** Para o escopo e o prazo do desafio, o ganho de simplicidade operacional e de clareza na
demonstração supera o apelo de "escala" do Kafka — não há requisito de throughput ou replay de log que
justifique a complexidade adicional.

**Trade-offs aceitos:** Sem replay de log nativo como no Kafka; se a vaga tivesse foco explícito em alto
volume/streaming, Kafka teria mais valor de sinalização técnica.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado no escopo atual do desafio.

---

<a id="d003"></a>
## D003 — Aplicação entra no Docker Compose só numa fase posterior

**Data:** 2026-07-10
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/001-infraestrutura-base.md
**Contexto:** O PRD exige, para o nível Sênior, que o Docker Compose final suba aplicação, banco de dados e
mensageria — mas não especifica em qual momento do desenvolvimento isso precisa acontecer.

**Alternativas consideradas:**
- **Já na Fase 1 (infraestrutura)** — detecta cedo problemas de rede/variáveis de ambiente entre containers,
  mas exige Dockerfile e rebuild de imagem a cada iteração das Fases 2/3, que ainda não têm nenhum código de
  domínio.
- **Fase posterior** — a app roda localmente via `./mvnw spring-boot:run` contra os serviços do compose
  durante o desenvolvimento de domínio; a containerização da app entra perto da entrega final, quando a API
  estabilizar.

**Decisão:** Fase posterior — a aplicação entra no `compose.yaml` mais perto da entrega final.

**Justificativa:** Evita rebuild de imagem a cada mudança de código durante as fases de domínio, sem abrir
mão do requisito do PRD, que fala do "compose final", não de cada fase intermediária.

**Trade-offs aceitos:** Problemas de rede/variáveis de ambiente entre a app containerizada e os demais
serviços só serão validados perto do fim do desenvolvimento.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se surgir sinal de que a integração via
container é arriscada (ex: configuração de rede complexa), antecipar essa validação antes da fase final.

---

<a id="d004"></a>
## D004 — Isolamento do banco de dados do Keycloak

**Data:** 2026-07-10
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/001-infraestrutura-base.md
**Contexto:** O Keycloak precisa de um banco relacional próprio para seus dados internos (usuários,
realms, clients). O `compose.yaml` já tem um serviço Postgres usado pela aplicação.

**Alternativas consideradas:**
- **Mesmo container, mesmo banco, schema separado** (`KC_DB_SCHEMA=keycloak`) — configuração mais simples
  (sem init script), mas menos isolamento — um único banco concentra dados de identidade e de domínio.
- **Mesmo container Postgres, banco dedicado** (`keycloak`), criado via script de init
  (`docker-entrypoint-initdb.d`) — isola os dados de identidade sem o custo de mais um processo.
- **Container Postgres dedicado só para o Keycloak** — isolamento total de infraestrutura, mas mais um
  processo Postgres para subir/gerenciar apenas para este desafio.

**Decisão:** Mesmo container Postgres, banco `keycloak` dedicado, criado via script de init.

**Justificativa:** Equilíbrio entre isolar dados de identidade do domínio acadêmico e não pagar o custo
operacional de mais um container Postgres, adequado ao escopo de 7 dias do desafio.

**Trade-offs aceitos:** Um script de init a mais para manter; se o container Postgres cair, tanto app
quanto Keycloak são afetados juntos (acoplamento de disponibilidade) — aceitável neste escopo.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se a solução evoluísse para produção real,
valeria considerar um Postgres gerenciado dedicado para identidade.

**Refinamento pós-security-review (2026-07-10):** a primeira implementação usava `POSTGRES_USER`/
`POSTGRES_PASSWORD` (o superusuário da aplicação) também como credencial do Keycloak em `KC_DB_USERNAME`/
`KC_DB_PASSWORD`. O `security-auditor` apontou que isso preservava o isolamento de *dados* (bancos
separados) mas não o isolamento de *privilégio* — um vazamento da credencial do Keycloak (ex.: log de
erro, dump de env do container) daria acesso de superusuário ao banco de domínio acadêmico também.
Corrigido: o script de init (`docker/postgres-init/01-create-keycloak-db.sh`) agora cria uma role dedicada
`keycloak_app` (via `KEYCLOAK_DB_USER`/`KEYCLOAK_DB_PASSWORD`, próprias, sem fallback óbvio), dona apenas do
banco `keycloak`. Também padronizado: nenhuma variável de senha no `compose.yaml` tem mais fallback
adivinhável (`guest`, `admin`, `secret`) — todas usam `${VAR:?...}`, exigindo um `.env` explícito.

---

<a id="d005"></a>
## D005 — Schema Postgres único (`public`) para todos os módulos

**Data:** 2026-07-10
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/001-infraestrutura-base.md
**Contexto:** A aplicação terá pelo menos dois módulos Modulith (acadêmico e um secundário de
notificação/auditoria). É preciso decidir se essa separação de módulo também se reflete em schemas
diferentes no Postgres.

**Alternativas consideradas:**
- **Schema único `public`** — todas as tabelas dos módulos convivem no mesmo schema; a fronteira de módulo
  é reforçada apenas no código (Spring Modulith, via `ApplicationModules.verify()`).
- **Schema por módulo** (ex: `academico`, `notificacao`) — isolamento também no nível de banco, mais
  próximo de uma futura extração para serviços separados, mas exige múltiplas *locations* no Flyway e
  cuidado com referências entre schemas.

**Decisão:** Schema único `public`.

**Justificativa:** Coerente com D001 — a fronteira de módulo já é verificada em tempo de teste pelo Spring
Modulith; replicá-la também no banco adicionaria complexidade de configuração do Flyway (múltiplas
locations, FKs cross-schema) sem ganho de isolamento adicional demonstrável no prazo do desafio.

**Trade-offs aceitos:** Menos "prova visual" de separação ao olhar só o schema do banco; mitigado por
convenção de nomenclatura de tabela por contexto e pela estrutura de pacotes no código.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se algum módulo precisar ser extraído de fato
como serviço separado no futuro, esse é o momento natural de revisitar e migrar para banco/schema próprio.

---

<a id="d006"></a>
## D006 — Mapeamento Keycloak ↔ Aluno adiado para a Fase 2

**Data:** 2026-07-10
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/001-infraestrutura-base.md
**Contexto:** O Keycloak gerencia usuários/credenciais internamente, mas a aplicação provavelmente
precisará associar um usuário autenticado a um Aluno (ou outro papel de domínio). A tabela `Aluno` ainda
não existe — está planejada para a Fase 2.

**Alternativas consideradas:**
- **Criar já nesta fase uma tabela solta** (`usuario_keycloak`, sem FK) e ligar de fato só na Fase 2 —
  mantém toda a "infra de auth" dentro da Fase 1, mas sem integridade referencial até lá.
- **Adiar totalmente para a Fase 2** — o mapeamento (coluna `keycloak_subject_id` ou tabela dedicada, a
  definir na spec da Fase 2) entra junto da própria migration que cria `Aluno`.

**Decisão:** Adiar para a Fase 2.

**Justificativa:** Evita criar uma tabela/coluna sem integridade referencial real que ficaria "pendurada"
até a Fase 2; nenhuma funcionalidade desta fase de infraestrutura depende desse mapeamento existir antes.

**Trade-offs aceitos:** Nenhum endpoint autenticado conseguirá resolver "qual Aluno é este usuário" antes
da Fase 2 — aceitável porque nenhuma rota de domínio existe ainda nesta fase.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

---

<a id="d007"></a>
## D007 — Grafana Loki + Promtail como backend de log centralizado

**Data:** 2026-07-10
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/002-observabilidade.md
**Contexto:** O PRD pede rastreabilidade mínima (logs estruturados, correlation/trace ID). Com Prometheus
(métricas) e Jaeger (tracing) já na stack, falta um jeito de visualizar logs de todos os containers sem
precisar rodar `docker logs`/entrar em cada container manualmente.

**Alternativas consideradas:**
- **Grafana Loki + Promtail** — Promtail coleta logs dos containers e envia para o Loki, que só indexa
  labels (não faz full-text indexing pesado); Grafana já está na stack como visualização de métricas, então
  passa a ser também o ponto único para logs — permitindo correlacionar log ↔ trace ↔ métrica pelo mesmo
  `traceId` na mesma tela (Explore do Grafana).
- **EFK/ELK** (Elasticsearch + Fluentd/Filebeat + Kibana) — busca full-text mais robusta e ecossistema mais
  conhecido no mercado, mas Elasticsearch sozinho já pede bem mais memória que toda a stack Loki, mais uma
  UI separada (Kibana) desacoplada do Grafana, e setup mais longo para o prazo de 7 dias do desafio.

**Decisão:** Grafana Loki + Promtail.

**Justificativa:** Correlação log↔trace↔métrica na mesma ferramenta (Grafana) é o ganho mais relevante para
a demonstração/entrevista; o footprint de recursos e o tempo de setup também pesaram a favor, dado o prazo
do desafio.

**Trade-offs aceitos:** Sem busca full-text nativa no conteúdo dos logs (Loki só indexa labels) — para o
volume e o propósito de um desafio técnico, isso não é uma limitação relevante.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o volume de logs ou a necessidade de busca
textual crescesse muito (cenário de produção real, múltiplas instituições), valeria reavaliar Elasticsearch
como backend de log.

**Refinamento pós-security-review (2026-07-11):** a implementação original centralizava logs de **todos**
os containers do compose (via bind mount de `/var/lib/docker/containers`, evitando `docker.sock` — ver
D017). O `security-auditor` apontou que essa forma de acesso (mesmo sem `docker.sock`) expõe
`config.v2.json` de cada container, que contém `Config.Env` em **texto claro** — ou seja, as credenciais de
qualquer container rodando na mesma máquina host (não só deste projeto) ficavam legíveis de dentro do
container do Promtail. Reduzido o escopo do Promtail para ler **só** o log da própria aplicação (ponte de
arquivo, D017), abrindo mão de centralizar logs de infraestrutura (Postgres/Keycloak/etc.) em troca de
eliminar essa exposição. Se a visibilidade de logs de infraestrutura for necessária no futuro, usar um
mecanismo com escopo mais restrito (ex: driver de log dedicado por serviço) em vez de acesso amplo ao
diretório de containers do host.

---

<a id="d008"></a>
## D008 — Stack de observabilidade atrás de um Docker Compose profile opcional

**Data:** 2026-07-10
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/002-observabilidade.md
**Contexto:** A stack de observabilidade (Prometheus, Grafana, Jaeger, Loki, Promtail) soma mais 5 serviços
aos 4 já existentes (postgres, redis, rabbitmq, keycloak). É preciso decidir se ela sobe sempre junto no
`docker compose up` do dia a dia ou fica atrás de um mecanismo opcional.

**Alternativas consideradas:**
- **Sempre junto (sem profiles)** — um único comando sobe tudo, mais simples de explicar, mas o ciclo de
  desenvolvimento diário fica pesado (9 containers) mesmo quando observabilidade não é o foco do momento.
- **Docker Compose profile `observability`** — `docker compose up` cotidiano continua leve (só os 4
  serviços de infra "core"); a stack de observabilidade só sobe com
  `docker compose --profile observability up`.

**Decisão:** Profile `observability` opcional.

**Justificativa:** Mantém o ciclo de desenvolvimento de domínio (Fases 2/3) leve por padrão, sem abrir mão
de ter a stack de observabilidade completa disponível sob demanda — e o uso de profiles em si já é um sinal
técnico razoável de organização do compose para quem for avaliar o repositório.

**Trade-offs aceitos:** Mais um comando/flag para documentar no README (`--profile observability`); quem
não ler a documentação pode achar que a observabilidade "não existe" por não aparecer no `docker compose up`
padrão.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado — reversível a qualquer
momento (bastaria remover a chave `profiles:` dos serviços).

---

<a id="d009"></a>
## D009 — Dashboard básico (JVM/HTTP) provisionado por padrão no Grafana

**Data:** 2026-07-10
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/002-observabilidade.md
**Contexto:** Além dos datasources (Prometheus, Jaeger, Loki), era preciso decidir se o Grafana já vem com
algum dashboard pronto ou se fica só com os datasources provisionados, sem nenhum dashboard, nesta fase.

**Alternativas consideradas:**
- **Só datasources agora** — nenhum dashboard de domínio existe ainda (sem métricas de matrícula/vaga para
  mostrar), então um dashboard pronto teria conteúdo só genérico (JVM/HTTP).
- **Provisionar já um dashboard básico (JVM/HTTP)** — usa as métricas padrão do Micrometer/Actuator
  (memória, GC, requests HTTP, etc.), disponível de cara, sem montar nada manualmente na UI do Grafana.

**Decisão:** Provisionar um dashboard básico de JVM/HTTP por padrão (contrariando o default sugerido pela
IA, que era só datasources).

**Justificativa (do Pablo):** Provar visualmente, desde já, que o pipeline métrica → Prometheus → Grafana
funciona ponta a ponta tem valor de demonstração imediato, mesmo antes de existir uma métrica de domínio —
evita depender de configurar isso manualmente na UI mais tarde, sob pressão de tempo.

**Trade-offs aceitos:** Mais um arquivo de provisionamento (JSON do dashboard) para manter; quando métricas
de domínio (ex: matrículas confirmadas/min) existirem, esse dashboard provavelmente precisará ser
estendido ou substituído.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

---

<a id="d010"></a>
## D010 — `oauth2-resource-server` em vez de `oauth2-client` para validar tokens

**Data:** 2026-07-11
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/003-seguranca-rbac-abac.md
**Contexto:** O `pom.xml` já trazia `spring-boot-starter-oauth2-client` (padrão do Spring Initializr), mas
a API é chamada por um frontend separado que faz o login e envia um Bearer token — a API só precisa validar
esse token, não iniciar um fluxo de login/manter sessão.

**Alternativas consideradas:**
- **Manter `oauth2-client`** — papel de "cliente OAuth2" (a própria app inicia login, redirect, sessão);
  não é o papel que uma API stateless validando Bearer tokens de terceiros precisa desempenhar.
- **Adicionar `oauth2-resource-server`** — papel de "resource server": valida assinatura/expiração do JWT
  (via `issuer-uri`/JWKS do Keycloak) e extrai authorities dele; é exatamente o papel de uma API REST
  protegida por Bearer token.

**Decisão:** Adicionar `spring-boot-starter-oauth2-resource-server`; remover `spring-boot-starter-oauth2-client`
do `pom.xml` por não ter nenhum uso na arquitetura atual (frontend separado, sem fluxo de login server-side
na própria API).

**Justificativa:** Reduz dependências não utilizadas e comunica corretamente, para quem ler o `pom.xml`, qual
é o papel real da aplicação no fluxo OAuth2/OIDC.

**Trade-offs aceitos:** Se um dia a própria API precisar iniciar um fluxo de login (ex: um painel
server-side rendered), `oauth2-client` precisaria voltar ao `pom.xml`.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado no escopo atual.

---

<a id="d011"></a>
## D011 — Papéis RBAC: ALUNO, SECRETARIA, ADMIN

**Data:** 2026-07-11
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/003-seguranca-rbac-abac.md
**Contexto:** O PRD descreve cadastro/edição/exclusão de alunos, cursos, disciplinas, turmas, matrícula de
aluno em turma e consultas por aluno/turma — sem especificar um modelo de papéis explícito. Era preciso
definir um conjunto de papéis RBAC razoável para o Keycloak antes de modelar o realm.

**Alternativas consideradas:**
- **ALUNO, SECRETARIA, ADMIN** — ALUNO se matricula/consulta as próprias matrículas; SECRETARIA
  cadastra/edita cursos, disciplinas, turmas (e matrículas de qualquer aluno); ADMIN acesso total (ex:
  gestão de usuários/papéis). Mapeia bem a separação de responsabilidades sugerida pelo PRD.
- **ALUNO, STAFF** (2 papéis) — junta SECRETARIA+ADMIN em um único papel; menos granularidade, mas evita
  modelar uma distinção sem regra de negócio realmente diferente entre os dois hoje no PRD.

**Decisão:** ALUNO, SECRETARIA, ADMIN (papéis de realm no Keycloak, não papéis de client).

**Justificativa:** Comunica melhor, para efeito de entrevista/avaliação, a separação de responsabilidades
que o PRD pede — mesmo que hoje SECRETARIA e ADMIN tenham sobreposição de permissões, a distinção fica
pronta para divergir quando regras mais finas (ex: gestão de usuários exclusiva de ADMIN) forem adicionadas.

**Trade-offs aceitos:** Nesta fase, sem entidades de domínio, a diferença prática entre SECRETARIA e ADMIN
é mínima — a distinção só ganha peso real na Fase 2.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se, na Fase 2, nenhuma regra realmente
diferenciar SECRETARIA de ADMIN, revisitar se vale a pena colapsar os dois papéis.

---

<a id="d012"></a>
## D012 — `PermissionEvaluator` customizado como mecanismo de ABAC

**Data:** 2026-07-11
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/003-seguranca-rbac-abac.md
**Contexto:** Era preciso definir como regras de posse/atributo (ABAC) seriam expressas no código — ex:
"aluno só vê a própria matrícula" (regra real, Fase 2) e "usuário só vê o próprio perfil" (exemplo
funcional desta fase).

**Alternativas consideradas:**
- **Bean customizado + SpEL direto** (`@PreAuthorize("@ownershipGuard.isOwnProfile(#id, authentication)")`)
  — mais direto/simples de ler e testar como um bean Spring comum, sem implementar uma interface formal do
  Spring Security.
- **`PermissionEvaluator` customizado** (`hasPermission(#id, 'PERFIL', 'READ')`) — segue o mecanismo
  "oficial" do Spring Security para autorização baseada em atributo/objeto, com dispatch por
  tipo+permissão; mais verboso para implementar, mas é o padrão mais reconhecível por quem já trabalhou
  com Spring Security e se generaliza melhor conforme mais tipos de recurso/permissão forem adicionados
  na Fase 2 (Matrícula, Turma, etc.), evitando um bean com um método ad-hoc para cada regra.
- **Keycloak Authorization Services (UMA/policies)** — descartada: chamada de rede por decisão de
  autorização, mais difícil de testar unitariamente, setup desproporcional ao prazo do desafio.

**Decisão (do Pablo, contrariando o default sugerido pela IA):** `PermissionEvaluator` customizado.

**Justificativa (do Pablo):** Prefere o mecanismo mais alinhado ao padrão "oficial" do Spring Security,
mesmo custando mais verbosidade agora, por generalizar melhor quando a Fase 2 trouxer múltiplos tipos de
recurso (Matrícula, Turma) precisando da mesma engrenagem de autorização por posse.

**Trade-offs aceitos:** Implementação inicial mais verbosa (registrar o bean, expor via
`MethodSecurityExpressionHandler`, dispatch por `targetType`/`permission`) do que a alternativa de bean+SpEL
direto, para um ganho que só se paga quando houver mais de um tipo de recurso.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado — é a via mais
extensível das três, revisão futura só se a complexidade do dispatch por tipo crescer além do previsto.

---

<a id="d013"></a>
## D013 — Client Keycloak confidential dedicado para o backend, já nesta fase

**Data:** 2026-07-11
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/003-seguranca-rbac-abac.md
**Contexto:** Era preciso decidir se o backend (que só valida tokens localmente via JWKS, sem precisar de
client id/secret próprio para isso) já ganha um client Keycloak confidential registrado no realm, ou se
isso fica para quando houver um uso concreto (ex: chamadas à Admin API do Keycloak, introspecção de token,
client-credentials para service-to-service).

**Alternativas consideradas:**
- **Nenhum client dedicado agora** — o backend, como resource server, só precisa do `issuer-uri`/JWKS;
  o realm só registraria o client do frontend (público, PKCE). Adiar o client do backend para quando
  houver uso real.
- **Já registrar um client confidential para o backend** — provisiona desde já o client (com secret) no
  realm exportado, mesmo sem um consumidor de código imediato.

**Decisão (do Pablo, contrariando o default sugerido pela IA):** já registrar um client confidential
`gestao-backend` no realm exportado.

**Justificativa (do Pablo):** Antecipar a necessidade em vez de ter que reabrir o realm export depois;
nenhum uso imediato foi especificado no momento da decisão.

**Implementação:** o client é registrado no realm export versionado com um **secret placeholder de
desenvolvimento** (não sensível — protege só um Keycloak local efêmero do `docker compose`, nunca exposto
publicamente), documentado em `.env.example` como `KEYCLOAK_BACKEND_CLIENT_SECRET`. Nenhum código desta
fase efetivamente consome esse secret (o backend continua validando JWTs localmente via `issuer-uri`); ele
fica pronto para quando um uso real (Admin API, introspecção, client-credentials) aparecer.

**Trade-offs aceitos:** Um secret "morto" (não usado por código) existe desde já no ambiente — superfície
nula de risco real (é um valor de desenvolvimento, não protege nada em produção), mas vale revisitar se,
ao final do desafio, ele seguir sem nenhum consumidor (nesse caso, considerar removê-lo do realm export
antes da entrega final, para não parecer uma peça solta sem explicação).

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se nenhuma fase futura vier a consumir esse
client, remover do realm export antes da entrega para não deixar configuração órfã.

**Refinamento pós-security-review (2026-07-11):** o `security-auditor` reforçou o risco já identificado
nesta decisão (secret sem consumidor) e apontou que a justificativa "Keycloak local efêmero" não é garantida
por nenhum controle técnico — nada impede que este `compose.yaml` seja reaproveitado como base de um
ambiente compartilhado. Corrigido: o secret não é mais uma string fixa no realm export; usa a substituição
nativa do Keycloak `${env.KEYCLOAK_BACKEND_CLIENT_SECRET}` (lida do ambiente do container `keycloak`, por
sua vez vinda de `.env`), então cada ambiente/clone do repositório tem um valor diferente, não mais o mesmo
valor idêntico versionado no Git.

**Refinamento — bug latente corrigido ao ganhar o primeiro consumidor real (2026-07-12, specs/010,
Fase 5c):** o secret finalmente ganhou um consumidor de código (`AdministracaoUsuarioService`, via
`client_credentials`) — e ao validar a autenticação de ponta a ponta, o placeholder
`${env.KEYCLOAK_BACKEND_CLIENT_SECRET}` registrado no refinamento anterior nunca havia sido de fato
exercitado (todo o período anterior, `serviceAccountsEnabled`/`standardFlowEnabled`/
`directAccessGrantsEnabled` estavam `false`, então nada tentava autenticar com esse secret). Dois problemas
reais, ambos bloqueadores, encontrados só agora: (1) o resolver de variável de ambiente do próprio Keycloak
(`AbstractFileBasedImportProvider`) usa o conteúdo entre `${...}` ao pé da letra via `System.getenv(...)` —
sem remover nenhum prefixo `env.`; a convenção `${env.X}` é do Spring/Quarkus (property sources), não deste
resolver específico do Keycloak, então o valor nunca resolvia. (2) a substituição de placeholders no
`--import-realm` só ocorre com a system property JVM `keycloak.migration.replace-placeholders=true`
(default `false`) — sem ela, o Keycloak importa a string literal `${VAR}` como o próprio valor do campo,
silenciosamente, sem erro. **Corrigido:** placeholder simplificado para `${KEYCLOAK_BACKEND_CLIENT_SECRET}`
(sem prefixo) e adicionado `JAVA_OPTS_APPEND=-Dkeycloak.migration.replace-placeholders=true` ao serviço
`keycloak` em `compose.yaml`. Alternativa descartada: hardcodear o secret real diretamente no JSON
versionado — violaria a proteção original desta decisão (mesmo valor em texto claro em todo clone do
repositório) só para contornar o bug, então não foi considerada uma opção razoável. Também concedido, no
mesmo commit: os client roles `view-users`/`manage-users`/`query-users` de `realm-management` ao service
account de `gestao-backend`, via o padrão nativo do Keycloak para isso (um usuário sintético
`service-account-gestao-backend` com `serviceAccountClientId`/`clientRoles` no realm export) — o campo
`serviceAccountClientRoles` diretamente no client, usado numa primeira tentativa, não existe no schema do
`ClientRepresentation` do Keycloak.

**Refinamento — segundo bug latente, achado ao rodar a suíte completa pela primeira vez (2026-07-13,
specs/010, Task 4 do plano de administração de usuários/papéis):** com o `AdministracaoUsuarioController`
implementado, `./mvnw clean verify` (gate formal da task) rodou pela primeira vez desde que
`KeycloakAdminConfig` ganhou um consumidor real (Task 3) — e todo `@SpringBootTest` da suíte (não só o novo
teste) passou a falhar na inicialização do contexto com
`PlaceholderResolutionException: Could not resolve placeholder 'KEYCLOAK_BACKEND_CLIENT_SECRET'`.
Causa: `gestao.keycloak-admin.client-secret=${KEYCLOAK_BACKEND_CLIENT_SECRET}` (Task 2) foi a única
property deste arquivo lida de variável de ambiente sem um valor de fallback — diferente do padrão já
usado para `JAEGER_OTLP_HTTP_PORT`, `KEYCLOAK_HTTP_PORT` e `PROMETHEUS_SCRAPE_PASSWORD` no mesmo
`application.properties` (todas com `${VAR:default-local}`). Isso quebrava qualquer `./mvnw test`/`verify`
rodado num shell que não tivesse `source .env` antes — inclusive o job `build` do CI, que só copia
`.env.example` para `.env` (`cp`, não `source`) antes de `./mvnw clean verify`. **Corrigido** aplicando o
mesmo padrão de fallback já estabelecido no arquivo:
`${KEYCLOAK_BACKEND_CLIENT_SECRET:changeme-keycloak-backend-secret-local}` — mesmo valor de desenvolvimento
já versionado em `.env.example`/`.env`, sem introduzir um segredo novo. Classificado como correção de bug
sem decisão de design nova (segue precedente já registrado nesta mesma decisão), não uma entrada própria no
log por si — mas registrado aqui como continuação direta do histórico deste secret, pelo mesmo motivo do
refinamento anterior: é exatamente o tipo de detalhe que só aparece quando um componente ganha seu primeiro
uso de ponta a ponta.

---

<a id="d014"></a>
## D014 — Exclusões do gate de cobertura JaCoCo

**Data:** 2026-07-11
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/004-fundacoes-transversais.md
**Contexto:** O gate de cobertura de 80% (JaCoCo `check`) precisa de exclusões explícitas para não penalizar
código que não deveria contar como "lógica testável" (config pura, DTOs, classe principal, getters/setters
gerados pelo Lombok) nem inflar o número artificialmente.

**Alternativas consideradas:**
- **Config + DTOs + classe principal + Lombok `@Generated`** — exclui só o que é estruturalmente
  não-testável ou irrelevante (`*Application`, `**/config/**`, `**/dto/**`/sufixos Request-Response, código
  marcado `@lombok.Generated` via `lombok.config`). Mantém a régua rigorosa sobre lógica de negócio e de
  segurança real.
- **Também excluir exceções e "glue" de segurança** (`SecurityConfig`, `MethodSecurityConfig`, classes de
  exceção customizadas) — reduziria ainda mais a régua, mas essas classes de segurança já tendem a ser
  exercitadas pelos testes de integração de RBAC/ABAC (spec 003); excluí-las poderia esconder um filtro mal
  configurado que os testes não cobrem.

**Decisão:** Config puro + DTOs + classe principal + Lombok `@Generated`.

**Justificativa:** Exclui só o que é genuinamente não-testável por natureza (wiring/config, data holders),
sem abrir uma exceção para código de segurança que já tem teste de integração cobrindo — evita esconder
uma regressão de RBAC/ABAC atrás de uma exclusão de cobertura complacente demais.

**Trade-offs aceitos:** Classes de configuração de segurança "puramente estruturais" (poucos branches) vão
contar para o denominador da régua de 80%, mesmo tendo pouca lógica própria — impacto esperado é pequeno.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se, na prática, classes de configuração
começarem a puxar a % para baixo sem ganho real de sinal de qualidade, revisitar a lista de exclusões.

---

<a id="d015"></a>
## D015 — Pasta única `db/migration` para todos os módulos

**Data:** 2026-07-11
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/004-fundacoes-transversais.md
**Contexto:** Com múltiplos módulos Modulith previstos (`academico`, `notificacao`, `security`), era preciso
decidir a organização física dos arquivos de migration do Flyway — distinto de D005 (que já decidiu o
schema único `public`; esta decisão é sobre estrutura de arquivo/pasta, não sobre schema).

**Alternativas consideradas:**
- **Pasta única `db/migration`**, com o módulo indicado na descrição do nome do arquivo (ex:
  `V1__academico_criar_tabela_aluno.sql`) — Flyway na configuração default (uma location), ordenação
  cronológica única, simples de entender.
- **Pasta por módulo** (`db/migration/academico`, `db/migration/notificacao`) com múltiplas *locations*
  configuradas — mais organizado visualmente conforme o número de módulos cresce, mas a ordem de aplicação
  entre pastas segue só o número de versão (não a pasta), podendo intercalar de forma não óbvia sem uma
  convenção extra de numeração por módulo.

**Decisão:** Pasta única `db/migration`, convenção de nome `V{sequencial}__{modulo}_{descricao}.sql`.

**Justificativa:** Coerente com D005 — a fronteira de módulo já é responsabilidade do código (Spring
Modulith), não da estrutura de arquivos; uma única location evita a armadilha de ordenação entre múltiplas
locations sem ganho real de organização neste volume de migrations.

**Trade-offs aceitos:** Conforme o número de migrations crescer muito, a pasta única pode ficar longa —
mitigado pelo prefixo de módulo no nome do arquivo, que já permite filtrar visualmente.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o volume de migrations por módulo crescer
muito e a leitura da pasta única ficar difícil, revisitar para pasta por módulo.

---

<a id="d016"></a>
## D016 — `type` (URI/URN) + propriedade `errorCode` no `ProblemDetail`

**Data:** 2026-07-11
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/004-fundacoes-transversais.md
**Contexto:** O tratamento padronizado de erros (RFC 7807/`ProblemDetail`) precisa de um identificador
estável para que o frontend trate erros programaticamente, não só exiba a mensagem em texto.

**Alternativas consideradas:**
- **Só uma propriedade customizada `errorCode`** (ex: `ERR_VALIDATION`), deixando `type` no default
  (`about:blank`) — mais simples, mas abre mão do uso que a própria RFC 7807 prevê para o campo `type`.
- **`type` como URI/URN estável + propriedade customizada `errorCode`** — `type` aponta para um
  identificador de catálogo (ex: `urn:gestao:erro:validacao-entrada`), como a RFC prevê; `errorCode`
  (string curta) via `setProperty(...)` dá ao frontend uma comparação direta sem parsear URI.

**Decisão:** `type` (URI/URN estável) + propriedade customizada `errorCode`.

**Justificativa:** Cobre a corretude da RFC 7807 (uso pretendido do campo `type`) e a ergonomia prática do
frontend (comparação direta por `errorCode`) ao mesmo tempo, sem custo de implementação relevante a mais.

**Trade-offs aceitos:** Duas formas de identificar o mesmo erro (`type` e `errorCode`) precisam ser mantidas
em sincronia — pequeno risco de divergência se um for atualizado sem o outro.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

**Refinamento pós-review (2026-07-11):** achados de `code-reviewer`/`security-auditor` sobre a implementação:
(1) a mensagem/errorCode do 403 estavam duplicados como string literal em `GlobalExceptionHandler` e em
`SecurityErrorHandlingConfig` — extraído `ProblemDetailFactory.accessDenied(path)` como ponto único; (2) a
cadeia de Basic Auth do scrape do Prometheus (`/actuator/prometheus`) não usava o `AuthenticationEntryPoint`
customizado, então uma falha de autenticação ali saía em formato diferente do resto da API — corrigido para
reaproveitar o mesmo bean; (3) `MethodArgumentNotValidException` só mapeava `getFieldErrors()`, descartando
silenciosamente erros de validação de nível de classe (`getGlobalErrors()`) — corrigido.

---

<a id="d017"></a>
## D017 — Ponte de logs via arquivo (não appender direto) para o Promtail alcançar a app

**Data:** 2026-07-11
**Origem:** 🤖 Default da IA, decidido durante a implementação sem pausar para perguntar (necessidade
emergente, descoberta empiricamente ao validar a spec 002 — não fazia parte das 3 decisões levantadas
previamente)
**Spec relacionada:** specs/002-observabilidade.md
**Contexto:** Ao validar a correlação log↔trace↔métrica (critério de aceite da spec 002), descobri que o
Promtail — configurado para ler `/var/lib/docker/containers/*/*-json.log` (D007, evitando `docker.sock`) —
nunca via os logs da aplicação, porque a app roda no **host**, fora do compose (D003). O driver `json-file`
do Docker só captura logs de containers; um processo do host não passa por ele.

**Alternativas consideradas:**
- **Ponte via arquivo** — a app grava logs estruturados (ECS) também em um arquivo (`logs/gestao.log`, via
  `logging.file.name`/`logging.structured.format.file`, ambos nativos do Spring Boot), montado
  somente-leitura no container do Promtail (`./logs:/var/log/gestao:ro`), com um scrape job dedicado.
- **Appender Logback direto para o Loki** (ex: biblioteca `loki-logback-appender`) — envia logs via HTTP
  diretamente da JVM para o Loki, sem depender de arquivo/container; funcionaria independente de onde a
  app roda, mas adiciona uma dependência nova ao projeto e mais uma configuração de biblioteca externa.

**Decisão:** Ponte via arquivo.

**Justificativa:** Zero dependência nova (usa só propriedades nativas do Spring Boot já usadas para o
console); consistente com a escolha já feita de não adicionar complexidade além do necessário (mesmo
espírito de D007 ao evitar `docker.sock`). Quando a aplicação entrar no `compose.yaml` (D003, fase futura),
reavaliar se este job de scrape dedicado ainda é necessário ou se a app passa a cair naturalmente no job
`docker-containers` (nesse caso, o job/arquivo específico pode ser removido).

**Trade-offs aceitos:** Mais um artefato (`logs/`, `.gitignore`, volume) a manter enquanto a app roda fora
do compose; duplica a escrita de log (console + arquivo) até essa fase futura.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Revisitar quando D003 for revertida (app
entrando no compose) — ver justificativa acima.

**Refinamento pós-security-review (2026-07-11):** o job `docker-containers` (que lia
`/var/lib/docker/containers` para centralizar logs de todos os serviços do compose, não só da app) foi
removido — ver refinamento equivalente em D007. O Promtail hoje só lê a ponte de arquivo desta decisão
(`gestao-app`), nada mais. Também removidos `trace_id`/`span_id` do stage `labels:` do Promtail (mantidos
só no conteúdo da linha): promovê-los a label do Loki cria uma série por valor único (alta cardinalidade),
e como `/actuator/health`/`/actuator/prometheus` são públicos, qualquer cliente não autenticado poderia
gerar esse volume — um vetor de esgotamento de recursos no Loki. A correlação log→trace no Grafana usa
`derivedFields` (regex sobre o conteúdo da linha, não sobre labels), então a funcionalidade não foi afetada.

---

<a id="d018"></a>
## D018 — Mapeamento DTO↔entidade manual, sem MapStruct

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/005-dominio-base.md
**Contexto:** Entidades JPA não podem vazar pela API (CLAUDE.md) — era preciso decidir a estratégia de
conversão entidade↔DTO para as 4 entidades de domínio (Aluno, Curso, Disciplina, Turma), sob prazo apertado
(entrega segunda-feira).

**Alternativas consideradas:**
- **MapStruct** — gera mappers em tempo de compilação, elimina boilerplate manual; exige coexistir com o
  Lombok já em uso (precisa de `lombok-mapstruct-binding`, ordem correta de annotation processors no
  `pom.xml`) — risco de integração real sob prazo apertado, para um ganho pequeno dado que são só 4
  entidades com DTOs simples (poucos campos, sem aninhamento profundo).
- **Mapeamento manual** — métodos de conversão simples, sem dependência nova, sem risco de integração,
  fácil de revisar em code review. Repetitivo entre as 4 entidades, mas o volume é pequeno.

**Decisão:** Mapeamento manual.

**Justificativa:** Sob prazo apertado, elimina qualquer risco de configuração de build (novo annotation
processor) para um ganho de produtividade pequeno neste volume de DTOs — o tempo economizado com MapStruct
seria consumido de volta se a integração com o Lombok desse qualquer problema.

**Trade-offs aceitos:** Repetição de código de mapeamento entre as 4 entidades — aceitável no volume atual;
revisitar se o número de entidades/DTOs crescer muito nas próximas fases.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se a Fase 3 (Matrícula) e além adicionarem
muitas outras entidades com DTOs, reavaliar MapStruct com mais tempo disponível.

---

<a id="d019"></a>
## D019 — Soft delete (campo `ativo`) para Aluno/Curso/Disciplina/Turma

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/005-dominio-base.md
**Contexto:** O PRD exige exclusão como um dos 4 verbos de CRUD para Aluno/Curso/Disciplina/Turma. Era
preciso decidir se "excluir" remove a linha de verdade (hard delete) ou apenas marca o registro como
inativo (soft delete) — decisão com implicação direta em como a Fase 3 (Matrícula, que referencia Turma via
FK) vai lidar com uma Turma "excluída" que já tem matrícula.

**Alternativas consideradas:**
- **Hard delete** — `DELETE` real na linha; mais simples agora, mas quebra na Fase 3 assim que uma Turma
  com matrícula precisar ser "excluída" (FK apontando para registro inexistente, ou seria necessário
  bloquear a exclusão nesse caso específico) — e perde histórico.
- **Soft delete** (campo booleano `ativo`) — excluir marca `ativo=false` em vez de remover a linha;
  preserva integridade referencial e histórico; mesmo padrão nas 4 entidades por consistência.

**Decisão:** Soft delete via campo booleano `ativo` (não timestamp `excluidoEm` — mais simples, suficiente
para "está ativo ou não").

**Justificativa:** Elimina de saída o problema de FK quebrada que o hard delete geraria na Fase 3, sem
custo de implementação adicional relevante agora.

**Trade-offs aceitos:** Toda consulta de listagem precisa filtrar `ativo=true` explicitamente (não é
automático); repositórios precisam disso desde já para não vazar registros "excluídos" nas listagens.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

---

<a id="d020"></a>
## D020 — Disciplina compartilhada entre Cursos (N:N), Turma com FK direto para Curso e Disciplina

**Data:** 2026-07-13
**Origem:** 🧑 Decisão do Pablo (contrariando o default sugerido pela IA)
**Spec relacionada:** specs/005-dominio-base.md
**Contexto:** Era preciso decidir a cardinalidade entre Curso, Disciplina e Turma — o PRD não especifica.

**Alternativas consideradas:**
- **Disciplina pertence a 1 Curso** — `Curso 1:N Disciplina 1:N Turma`, sem tabela de junção; Turma
  referencia só Disciplina, Curso fica acessível de forma transitiva. Mais simples, sem migration extra.
- **Disciplina compartilhada entre Cursos (N:N)** — mais realista (ex: "Cálculo I" comum a Engenharia e
  Física); exige tabela de junção `curso_disciplina`, e Turma passa a precisar de FK direto para Curso E
  Disciplina (para saber "turma desta disciplina, dentro deste curso específico").

**Decisão (do Pablo, contrariando o default sugerido pela IA):** Disciplina compartilhada entre Cursos
(N:N); Turma com FK direto para `curso_id` e `disciplina_id`.

**Justificativa (do Pablo):** Modelagem mais próxima da realidade de um sistema acadêmico — uma disciplina
genuinamente pode ser oferecida em mais de um curso.

**Implementação:** tabela de junção `curso_disciplina` (FK para `curso` e `disciplina`); `Turma` com
`curso_id` e `disciplina_id` como FKs diretos; regra de negócio nova (registrada como parte desta decisão,
não uma entrada separada): criar uma Turma exige que o par `(curso_id, disciplina_id)` já exista em
`curso_disciplina` — validado no service, retornando 409 (`ConflitoRegraNegocioException`, D016) se a
disciplina não estiver de fato associada àquele curso.

**Trade-offs aceitos:** Mais uma tabela e mais uma migration sob prazo apertado; mais uma validação de
negócio a testar (par curso/disciplina inválido).

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

---

<a id="d021"></a>
## D021 — Leitura de Curso/Disciplina/Turma aberta a qualquer autenticado

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, aceito sem alteração (opção recomendada, confirmada pelo usuário entre
alternativas explícitas, sem justificativa adicional própria)
**Spec relacionada:** specs/005-dominio-base.md
**Contexto:** Era preciso decidir o RBAC de leitura (`GET`) dos endpoints de Curso/Disciplina/Turma — se
`ALUNO` tem acesso já nesta fase, ou só quando Matrícula (Fase 3) definir o que ele precisa ver.

**Alternativas consideradas:**
- **Leitura restrita a SECRETARIA/ADMIN por agora** — mais conservador, mas exige revisitar o RBAC desses
  3 controllers na Fase 3, quando o aluno precisar navegar turmas disponíveis para se matricular.
- **Leitura aberta a qualquer autenticado** (`ALUNO`/`SECRETARIA`/`ADMIN`); escrita (criar/editar/excluir)
  continua restrita a `SECRETARIA`/`ADMIN`.

**Decisão:** Leitura aberta a qualquer autenticado.

**Justificativa:** Evita reabrir e re-testar o RBAC desses 3 controllers na Fase 3 — o aluno precisará
navegar turmas disponíveis para se matricular, então essa necessidade já é conhecida agora.

**Trade-offs aceitos:** Nenhum identificado — escrita continua protegida, e não há dado sensível em
Curso/Disciplina/Turma que justifique restringir leitura.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

---

<a id="d022"></a>
## D022 — Modelagem menor: status de Turma, unicidade, vínculo Aluno↔Keycloak

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, decidido durante a implementação sem levantar como pergunta separada (baixo
risco/baixa ambiguidade, sinalizado na spec para o usuário revisar, não bloqueante)
**Spec relacionada:** specs/005-dominio-base.md
**Contexto:** Um conjunto de decisões de modelagem menores, agrupadas aqui para não inflar o log com
entradas triviais (skill decision-log):

- **Status de Turma:** enum (`StatusTurma { ABERTA, FECHADA }`) em vez de booleano — mesmo padrão já usado
  para o status de Matrícula (`PENDENTE`/`CONFIRMADA`/`CANCELADA`, PRD §02), mais extensível que um booleano
  se novos estados surgirem depois (ex: `EM_ANDAMENTO`, `ENCERRADA`), sem precisar reinterpretar um campo
  booleano existente.
- **Unicidade:** `Curso.codigo` único; `Disciplina.codigo` único globalmente (é um item de catálogo
  compartilhável agora, D020); `Turma.codigo` único dentro do par `(curso_id, disciplina_id)`.
- **Regra de negócio implícita:** não é possível inativar (soft delete) um Curso com Disciplina ativa
  vinculada via `curso_disciplina`, nem inativar uma Disciplina com Turma ativa vinculada — evita órfãos
  lógicos sob um "pai" inativo. Erro 409 (`ConflitoRegraNegocioException`, D016).
- **Vínculo Aluno↔Keycloak (executa D006):** a migration que cria `Aluno` já inclui a coluna
  `keycloak_subject_id` (nullable, unique) prevista em D006 ("mapeamento adiado para quando Aluno
  existir") — evita uma migration adicional na Fase 3. Nenhum código consome essa coluna ainda (sem
  self-service de aluno nesta fase, D021 cobre só leitura de Curso/Disciplina/Turma).

**Decisão:** Conforme detalhado acima.

**Justificativa:** Escolhas de baixo risco, consistentes com padrões já estabelecidos no projeto
(enum de status como em Matrícula; execução de D006 já planejada); agrupadas para não fragmentar o log.

**Trade-offs aceitos:** Nenhum identificado além do já mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o usuário discordar de alguma dessas
escolhas específicas ao revisar a spec, ajustar antes da implementação.

---

<a id="d023"></a>
## D023 — Correções decorrentes do code-reviewer/security-auditor da spec 005

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA — achados de review, corrigidos sem levantar como pergunta separada (correção
de bug/robustez, não decisão com alternativas razoáveis em aberto)
**Spec relacionada:** specs/005-dominio-base.md
**Contexto:** `code-reviewer` e `security-auditor` (etapas 7/8 do fluxo do CLAUDE.md) revisaram o diff
completo da spec 005 em paralelo. Convergiram no mesmo achado principal (divergindo só na severidade
atribuída) e o `code-reviewer` levantou mais 4 achados menores. Resumo e resolução:

1. **[Divergência de severidade: HIGH para o code-reviewer, LOW para o security-auditor] `desvincularDisciplina`
   sem guarda contra `Turma` vinculada ao par.** A remoção da linha em `curso_disciplina` violava a FK
   composta de `turma` (V5) quando havia Turma (ativa ou inativa) referenciando aquele par
   `(curso_id, disciplina_id)` — `GlobalExceptionHandler` não trata `DataIntegrityViolationException`, então
   a requisição vazava um 500 genérico em vez do 409 de negócio usado em todo o resto da API. Sem
   exploração de segurança (sem vazamento de dado, sem bypass de autorização — daí a divergência de
   severidade), mas viola diretamente a exigência de tratamento de erro padronizado do PRD/CLAUDE.md.
   **Corrigido:** novo método `TurmaRepository.existsByCursoIdAndDisciplinaId` (sem filtro `ativo=true` —
   mesmo uma Turma inativada mantém a linha física, então ainda bloqueia a FK), checado em
   `CursoService.desvincularDisciplina` antes da remoção, lançando `ConflitoRegraNegocioException` (409).
   Teste novo: `CursoServiceTest#desvincularDisciplina_comTurmaVinculadaAoPar_lancaConflito`. Validado
   também ponta a ponta com token real (retornou 409 com `ProblemDetail` correto, não 500).
2. **[MEDIUM, code-reviewer] Unicidade case-sensitive em `Aluno.email`/`Curso.codigo`/`Disciplina.codigo`.**
   `existsByEmail`/`existsByCodigo` e as constraints `UNIQUE` do banco são case-sensitive; nada normalizava
   o valor antes de comparar/persistir, permitindo duplicados de fato (ex: `"ana@x.com"` e `"Ana@X.com"`,
   `"ADS"` e `"ads"`). **Corrigido:** normalização na borda do service — email: `trim().toLowerCase()`;
   código (Curso/Disciplina/Turma, mesma classe de problema): `trim().toUpperCase()` — antes de checar
   unicidade e antes de persistir. Validado ponta a ponta com token real (`"caseins"` seguido de `"CASEINS"`
   → 409).
3. **[MEDIUM/LOW, code-reviewer] N+1 ao listar Turma.** `Turma.curso`/`Turma.disciplina` são `LAZY` e
   acessados no mapeamento do controller (fora da transação do service) — funcionava só por depender do
   default `spring.jpa.open-in-view=true` (não desabilitado neste projeto), e gerava 1+2N selects por
   listagem. **Corrigido:** `@EntityGraph(attributePaths = {"curso", "disciplina"})` em
   `TurmaRepository.findByAtivoTrue()`/`findByIdAndAtivoTrue()`.
4. **[LOW, code-reviewer] Status HTTP implícito em `POST /api/cursos/{id}/disciplinas/{disciplinaId}`.**
   Único `POST` das 4 controllers sem `@ResponseStatus` explícito (caía no default 200 do Spring MVC, por
   não criar um recurso novo no sentido REST — mas sem declarar isso, lia como descuido, não decisão).
   **Corrigido:** `@ResponseStatus(HttpStatus.OK)` explícito.
5. **[LOW, code-reviewer, não corrigido — risco documentado] `Curso`/`Disciplina` sem `equals`/`hashCode`
   customizado, usadas como elementos de `Set` (`@ManyToMany`).** Hoje não é um bug observável: toda
   manipulação do `Set<Disciplina>` acontece dentro de uma única transação/sessão, com identidade de objeto
   Java garantida pelo persistence context do Hibernate. Vira um problema latente só se uma entidade dessas
   for usada em um `Set` fora desse contexto (ex: entidades vindas de duas queries/sessões diferentes).
   **Decisão: não corrigir agora.** O padrão recomendado (equals baseado em id, com fallback de identidade
   para entidades transientes) tem armadilhas conhecidas (hashCode mutável se a entidade for adicionada a um
   `HashSet` antes de persistir) e a Lombok `@EqualsAndHashCode` simples não cobre isso corretamente — o
   custo de acertar essa implementação sob o prazo apertado desta fase não se justifica para um risco que
   não é hoje alcançável por nenhum caminho de código existente. Fica como risco conhecido para revisitar
   se/quando entidades `academico` passarem a atravessar sessões (ex: cache de segundo nível, eventos
   assíncronos carregando entidades destacadas).

**Trade-offs aceitos:** Item 5 foi conscientemente deixado sem correção (ver justificativa acima) —
não é uma omissão, é uma escolha explícita de escopo sob prazo.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Revisitar o item 5 se `academico` passar a
usar cache de segundo nível do Hibernate ou entidades atravessarem múltiplas sessões/serialização.

---

<a id="d024"></a>
## D024 — Mecanismo de proteção de vaga: UPDATE condicional atômico + `@Version`

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário (pesquisa solicitada explicitamente antes de decidir —
"quero uma solução robusta de mercado, estilo grande e-commerce")
**Spec relacionada:** specs/006-matricula.md
**Contexto:** O escopo desta fase já fixava "lock otimista via `@Version` do JPA" como mecanismo de
concorrência (decisão de sequenciamento anterior do Pablo — a comparação aprofundada com lock pessimista/
outras estratégias fica para a Fase 7). Faltava decidir *como*, exatamente, o consumo de vaga usa esse
mecanismo: o padrão "tutorial" de JPA (carregar entidade → checar em Java → `save()`, deixando só o
`@Version` pegar conflito no commit) vs. um `UPDATE` condicional atômico que checa e decrementa no mesmo
statement SQL.

**Pesquisa realizada (ver mensagem correspondente na conversa para as fontes completas):** para sistemas de
inventário com contenção moderada (não é o caso de flash sale com dezenas de milhares de req/s pela mesma
unidade — turmas têm capacidade modesta), o padrão consolidado como mais robusto é o `UPDATE` condicional
(`WHERE quantidade < limite`), porque elimina a janela entre leitura e escrita por construção — o próprio
lock de linha do banco durante o `UPDATE` serializa tentativas concorrentes. Lock otimista por versão puro
é descrito como ideal para "alta concorrência, baixo conflito" (linhas *diferentes*), não para a disputa
pela última vaga da *mesma* linha, onde tende a gerar mais round-trips desperdiçados.

**Alternativas consideradas:**
- **`UPDATE` condicional atômico + `@Version`** — `UPDATE turma SET vagas_ocupadas = vagas_ocupadas + 1,
  version = version + 1 WHERE id = ? AND version = ? AND vagas_ocupadas < limite_vagas` (via
  `@Modifying @Query` do Spring Data). Zero linhas afetadas → uma re-consulta (só no caminho de falha)
  diferencia "vagas esgotadas" de "conflito de versão por edição concorrente de outro campo da Turma".
- **Carregar entidade → checar em Java → `save()`** — mais próximo do fluxo JPA convencional, mas o
  `UPDATE` gerado pelo Hibernate só checa a versão, não a condição de negócio diretamente; menos preciso
  para diferenciar os dois motivos de falha.

**Decisão:** `UPDATE` condicional atômico, com `@Version` mantido na entidade `Turma` para proteger
concorrência em outros campos (ex: edição administrativa simultânea).

**Justificativa:** Elimina a janela de corrida por construção (não depende de nenhuma lógica em Java entre
ler e escrever); permite diferenciar precisamente "vagas esgotadas" (regra de negócio, 409
`ConflitoRegraNegocioException`, `VAGAS_ESGOTADAS`) de "conflito de concorrência genérico" (edição
simultânea de outro campo, também 409, código diferente); é o padrão que a pesquisa de mercado aponta como
mais robusto nessa escala de contenção, sem introduzir a complexidade de Redis/fila/reserva com TTL
(apropriada só em escala de flash sale, fora do escopo desta fase e desproporcional à carga real de
matrícula em turma).

**Trade-offs aceitos:** Query nativa/JPQL de `UPDATE` em vez de `save()` convencional — menos "idiomático"
ao estilo do resto do projeto (que usa `save()`/dirty checking em todo o resto do CRUD), mas justificado
pela criticidade desta regra específica (PRD §06, critério eliminatório).

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se a carga real de matrícula concorrente
crescer para uma escala de "flash sale" (milhares de req/s pela mesma turma), revisitar na Fase 7 com
reserva via Redis + fila, conforme a pesquisa indicou como padrão nessa escala.

---

<a id="d025"></a>
## D025 — Contador de vagas ocupadas: campo incremental, não `COUNT` calculado

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário (mesma consulta de D024)
**Spec relacionada:** specs/006-matricula.md
**Contexto:** Decorrência direta de D024 — o `UPDATE` condicional atômico precisa de um campo na própria
linha de `Turma` para checar/decrementar; decidir se esse campo existe (`vagas_ocupadas`) ou se é sempre
recalculado via `COUNT(*)` de `Matricula` com `status = 'CONFIRMADA'`.

**Alternativas consideradas:**
- **Campo `vagas_ocupadas` (int) na Turma** — atualizado pelo próprio `UPDATE` condicional de D024, na
  mesma linha protegida por `@Version`; `CHECK (vagas_ocupadas <= limite_vagas)` e
  `CHECK (vagas_ocupadas >= 0)` como backstop de banco (mesmo padrão de `CHECK (limite_vagas > 0)` da
  Fase 2).
- **`COUNT(*)` de matrículas `CONFIRMADA`** — nunca dessincroniza da realidade, mas quebra a atomicidade do
  `UPDATE` condicional (a condição dependeria de outra tabela); exigiria lock pessimista ou subquery no
  `WHERE`, mais caro e menos direto.

**Decisão:** Campo incremental `vagas_ocupadas` na Turma.

**Justificativa:** Consequência direta de D024 — a atomicidade do `UPDATE` condicional depende de checar e
escrever a mesma linha num único statement.

**Trade-offs aceitos:** Duplicação de informação (o número de matrículas `CONFIRMADA` também é derivável da
tabela `matricula`) — mitigado pelo `CHECK` constraint como backstop de integridade.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado em
D024.

---

<a id="d026"></a>
## D026 — Matrícula cancelada permite nova matrícula na mesma turma

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário
**Spec relacionada:** specs/006-matricula.md
**Contexto:** Regra de unicidade "aluno não pode se matricular duas vezes na mesma turma" (PRD §02) precisa
decidir se uma matrícula `CANCELADA` conta como "já matriculado" para esse efeito.

**Alternativas consideradas:**
- **Permite recriar** — unicidade checada só contra matrículas `PENDENTE`/`CONFIRMADA`; cobre "cancelei por
  engano" ou "desisti e mudei de ideia". Implementação: índice único parcial,
  `UNIQUE (aluno_id, turma_id) WHERE status <> 'CANCELADA'`.
- **Cancelamento definitivo** — unicidade vale contra qualquer matrícula existente, incluindo `CANCELADA`;
  `UNIQUE (aluno_id, turma_id)` simples.

**Decisão:** Permite recriar após cancelamento.

**Justificativa:** Mais alinhado à intenção prática do PRD — um cancelamento é uma decisão do momento, não
necessariamente permanente; bloquear matrícula futura na mesma turma por causa de um cancelamento antigo
seria uma restrição não pedida explicitamente e prejudicial à experiência do aluno.

**Trade-offs aceitos:** Índice único parcial em vez de `UNIQUE` simples — sintaxe específica do Postgres
(`CREATE UNIQUE INDEX ... WHERE ...`), levemente menos portável, mas o projeto já é Postgres-only (D001).

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado.

---

<a id="d027"></a>
## D027 — Confirmação de matrícula restrita a SECRETARIA/ADMIN

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário
**Spec relacionada:** specs/006-matricula.md
**Contexto:** RBAC de `POST /api/matriculas/{id}/confirmar` — decidir se é um passo administrativo (staff)
ou se o próprio aluno pode confirmar a própria matrícula.

**Alternativas consideradas:**
- **Só SECRETARIA/ADMIN** — `PENDENTE→CONFIRMADA` como gate administrativo (ex: validação de documentos/
  pagamento fora do escopo desta fase, mas modelado como possível).
- **ALUNO também pode confirmar a própria matrícula** — self-service completo.

**Decisão:** Só SECRETARIA/ADMIN.

**Justificativa:** A existência de um status `PENDENTE` distinto de `CONFIRMADA` sugere que criar e
confirmar são passos distintos, possivelmente por atores diferentes — se a confirmação fosse automática/
self-service, um único status já bastaria. ALUNO cria (`POST /api/matriculas`) e cancela a própria
(`POST /api/matriculas/{id}/cancelar`); SECRETARIA/ADMIN confirmam.

**Trade-offs aceitos:** Fluxo de matrícula não é 100% self-service para o aluno — depende de uma ação de
staff para consumir a vaga. Aceito porque reflete melhor o desenho de um sistema acadêmico real.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o PRD (ou uma clarificação futura) indicar
confirmação automática/self-service, revisitar.

---

<a id="d028"></a>
## D028 — Máquina de estados de Matrícula e resposta a conflito de vaga

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado (grupado por baixo risco, mesmo padrão de D022)
**Spec relacionada:** specs/006-matricula.md
**Contexto:** Duas decisões relacionadas, agrupadas: (1) a máquina de estados completa de `Matricula`,
incluindo os casos de borda que o desenho óbvio (PENDENTE→CONFIRMADA, PENDENTE→CANCELADA,
CONFIRMADA→CANCELADA) não cobre; (2) o que a API faz quando uma requisição perde a disputa pela última
vaga.

**Decisão — máquina de estados:**
- `PENDENTE → CONFIRMADA`: consome vaga (D024/D025).
- `PENDENTE → CANCELADA`: não mexe em vaga (nunca ocupou).
- `CONFIRMADA → CANCELADA`: libera vaga.
- `CONFIRMADA → CONFIRMADA`: idempotente, não é erro — confirmar de novo uma matrícula já confirmada
  retorna 200 sem efeito colateral (evita expor um detalhe de implementação como erro de negócio se o
  cliente reenviar por timeout/retry de rede).
- `CANCELADA` é terminal: qualquer transição a partir dela (confirmar ou cancelar de novo) é 409
  (`ConflitoRegraNegocioException`, `TRANSICAO_INVALIDA`).
- `CONFIRMADA → PENDENTE`: não existe, não modelado.

**Decisão — conflito de vaga:** 409 direto ao cliente, sem retry automático no backend. Consequência de
D024: com o `UPDATE` condicional atômico, "perder a última vaga" não é mais um lock conflict genérico — é
um resultado de negócio (zero linhas afetadas porque `vagas_ocupadas` já bateu o limite). Retry automático
não ajudaria nesse caso (o resultado não muda) e poderia até ser injusto (se um cancelamento de outro aluno
abrir vaga entre tentativas do retry, isso furaria fila de forma não determinística em vez de reflitir a
ordem real das requisições). Retry só faria sentido no caso raro de conflito de versão por edição
concorrente de outro campo da Turma — valor baixo, não justifica a complexidade.

**Justificativa:** Ambas as escolhas seguem diretamente do desenho já decidido em D024 (não são decisões
independentes de alto risco) — apresentadas junto com a spec para revisão, não bloqueantes.

**Trade-offs aceitos:** Nenhum identificado além do já mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado em
D024.

---

<a id="d029"></a>
## D029 — Sequenciamento de mensageria: eventos internos via Spring Modulith nesta fase, RabbitMQ na Fase 4

**Data:** 2026-07-14
**Origem:** 🧑 Decisão do Pablo (dada explicitamente na instrução desta fase, registrada aqui antes de
implementar conforme solicitado)
**Spec relacionada:** specs/006-matricula.md
**Contexto:** O PRD exige mensageria assíncrona real (RabbitMQ) com um consumidor em módulo/serviço
separado (ver CLAUDE.md, "Arquitetura-alvo"). Era preciso decidir a ordem: implementar a publicação de
eventos de domínio (`MatriculaCriada`/`Confirmada`/`Cancelada`) e o broker externo juntos nesta fase, ou
sequenciar em duas etapas.

**Decisão:** Publicar os eventos nesta fase só internamente, via `ApplicationEventPublisher` do
Spring/Modulith, consumidos por um `@ApplicationModuleListener` no módulo secundário `notificacao`. A
externalização para o RabbitMQ como broker real fica para a Fase 4.

**Justificativa (do Pablo):** A publicação interna agora é o que permite a Fase 4 apenas configurar a
externalização do Spring Modulith para o broker (`spring-modulith-events-amqp` ou equivalente), sem
reescrever a lógica de publicação/consumo em si — reduz retrabalho e separa "a regra de negócio publica o
evento certo, no momento certo" (testável agora, sem infraestrutura externa) de "o evento atravessa
processos via broker" (Fase 4, com sua própria configuração de exchange/fila/idempotência de consumo).

**Trade-offs aceitos:** O `@ApplicationModuleListener` desta fase é síncrono-dentro-do-processo (mesma
JVM); não testa nada sobre falha de broker, mensagem duplicada, ou consumidor fora do ar — esses cenários
só existem a partir da Fase 4.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum — sequenciamento já é a decisão
deliberada.

---

<a id="d030"></a>
## D030 — Decisões menores agrupadas: extensão do ABAC, vínculo Aluno↔Keycloak, RBAC de listagem por turma, ferramenta de E2E

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado durante a fase de planejamento (grupado por
baixo risco, mesmo padrão de D022)
**Spec relacionada:** specs/006-matricula.md
**Contexto:** Conjunto de decisões técnicas menores identificadas ao planejar esta fase, agrupadas para não
inflar o log:

- **Extensão do `PermissionEvaluator` único** (não um novo por tipo) — a spec 003 já previa essa escolha
  para quando Matrícula existisse, e `MethodSecurityConfig` só injeta um bean `PermissionEvaluator`. Dois
  `targetType` novos no mesmo componente: `"ALUNO"` + permissão `"MATRICULAR"` (resolve o Aluno pelo id,
  compara `keycloakSubjectId` ao subject do JWT — usado em `POST /api/matriculas` via `#request.alunoId()`
  e em `GET /api/alunos/{alunoId}/matriculas`); `"MATRICULA"` + permissão `"GERENCIAR"` (resolve a
  Matrícula pelo id, compara `matricula.aluno.keycloakSubjectId` ao subject — usado em consultar/cancelar
  uma matrícula específica).
- **Vínculo Aluno↔Keycloak passa a ser preenchível** — gap encontrado: a coluna `keycloak_subject_id`
  existe desde a Fase 2 (D006/D022) mas nenhum endpoint a expunha. `AlunoRequest`/`AlunoService` passam a
  aceitar um `keycloakSubjectId` opcional, só editável por SECRETARIA/ADMIN (que já são os únicos que
  mexem em Aluno) — vincula o registro acadêmico à conta de login. Self-service de vínculo (o próprio
  aluno reivindicar seu registro) continua fora de escopo.
- **`GET /api/turmas/{turmaId}/matriculas` (lista completa de uma turma) é staff-only** (RBAC simples,
  `hasRole('SECRETARIA') or hasRole('ADMIN')`, sem ABAC) — um aluno não deveria ver quem mais está
  matriculado na turma (vazaria dados de outros alunos).
- **E2E via `e2e/matricula-flow.sh`**, reaproveitando o padrão bash/curl já estabelecido em
  `e2e/smoke-test.sh` (specs/004) em vez de introduzir uma ferramenta nova (REST Assured/Testcontainers)
  só para isso. O CI (`.github/workflows/ci.yml`) já sobe o compose completo com Keycloak real e os
  usuários de teste (`aluno.teste`/`secretaria.teste`) antes de rodar o smoke test — o novo script busca
  tokens reais desse Keycloak e exercita a API via HTTP real (não JWT simulado do MockMvc), o que valida a
  cadeia OAuth2 Resource Server inteira, não só o `PermissionEvaluator`. Adicionado como novo step no
  `ci.yml`, logo após o smoke test existente.

**Decisão:** Conforme detalhado acima.

**Justificativa:** Escolhas de baixo risco que reaproveitam padrões já estabelecidos no projeto (mecanismo
de ABAC da spec 003; convenção de E2E via bash/curl da spec 004) em vez de introduzir componentes novos sem
necessidade, sob prazo apertado.

**Trade-offs aceitos:** Nenhum identificado além do já mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o número de `targetType` no
`PermissionEvaluator` único crescer muito nas próximas fases, revisitar a divisão em um `PermissionEvaluator`
por tipo (já sinalizado como opção pela spec 003).

---

<a id="d031"></a>
## D031 — Correções decorrentes do code-reviewer/security-auditor da spec 006

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA — achados de review, corrigidos sem levantar como pergunta separada (correção
de bug/robustez, não decisão com alternativas razoáveis em aberto)
**Spec relacionada:** specs/006-matricula.md
**Contexto:** `code-reviewer` e `security-auditor` revisaram o diff completo desta spec em paralelo, com
atenção explícita ao mecanismo de proteção de vaga (D024) e ao primeiro uso real de ABAC do projeto. Nenhum
achado Crítico/Alto; um achado Médio-Alto e um achado Médio (mesma classe de problema encontrada por ambos
os revisores, de ângulos diferentes) mereciam correção. Resumo e resolução:

1. **[Médio-Alto, code-reviewer + Médio, security-auditor] TOCTOU nas checagens de unicidade em código
   (`existsBy...`) degradava para 500 em vez de 409 sob concorrência real.** Duas ocorrências do mesmo
   padrão: `MatriculaService.criar` (duplicidade aluno/turma, backstop no índice único parcial de V6) e
   `AlunoService.criar/editar` (`keycloakSubjectId`, backstop na constraint UNIQUE de V1). Sob duas
   requisições concorrentes (ex: duplo clique), ambas podem passar a checagem `existsBy...` e uma delas
   viola a constraint no `INSERT`/`UPDATE`; `GlobalExceptionHandler` não tinha handler para
   `DataIntegrityViolationException`, então caía no handler genérico (500). Este é o mesmo padrão já
   apontado como risco conhecido em `CursoService` na Fase 2 (comentário em código, nunca endereçado
   globalmente). **Corrigido com um handler genérico** (`GlobalExceptionHandler.handleConflitoIntegridade`,
   409, `errorCode=DATA_INTEGRITY_CONFLICT`, mensagem sem detalhe de constraint/tabela/coluna) — uma rede
   de segurança única para qualquer violação de unicidade sob corrida em qualquer service do projeto, em
   vez de corrigir cada `existsBy...` individualmente com lock pessimista ou upsert condicional (fora de
   escopo/desproporcional dado o prazo — a checagem em código continua sendo o caminho feliz 99,9% do
   tempo; isto é só a rede de segurança para a janela de corrida rara). Teste novo:
   `GlobalExceptionHandlerTest#conflitoDeIntegridadeVira409SemVazarDetalheDoBanco`.
2. **[Médio, code-reviewer, verificado empiricamente com SQL logging] `MatriculaService.confirmar`/
   `cancelar` reintroduziam N+1 ao montar a resposta, mascarado só por `open-in-view=true`.** O
   `clearAutomatically=true` do `consumirVaga`/`liberarVaga` (necessário para a re-consulta correta de
   VAGAS_ESGOTADAS vs. CONFLITO_CONCORRENCIA, D024) detacha `matricula` do persistence context; a correção
   inicial (re-buscar via `findById` com `@EntityGraph` após `save()`) **não funcionava de verdade** — o
   `save()` faz `merge()`, que sem `cascade=MERGE` na associação cria proxies não inicializados para
   aluno/turma, e uma nova consulta via `findById` não ajudava porque o identity map do Hibernate já
   devolvia a mesma instância recém-mergeada em cache, sem re-executar a query com o `@EntityGraph`.
   Confirmado com log de SQL (`org.hibernate.SQL=DEBUG`) rodando `MatriculaIntegrationTest`: a correção
   inicial ainda gerava 2 selects extras (lazy load de aluno/turma) após o `update matricula`. **Corrigido
   de verdade** trocando `save()` por: recarregar uma instância MANAGED via `buscarPorId` (que usa
   `@EntityGraph`) **antes** de mutar os campos, deixando o dirty checking do Hibernate gerar o `UPDATE` no
   commit — sem `merge()`, aluno/turma nunca são substituídos por proxies. Reverificado com o mesmo log de
   SQL: nenhuma query extra após o `UPDATE turma`/`UPDATE matricula`. Isso também elimina a dependência de
   `spring.jpa.open-in-view=true` para este fluxo especificamente (o código agora está correto mesmo se
   open-in-view for desabilitado no futuro).
3. **[Baixo, security-auditor] `keycloakSubjectId` aceitava string em branco (`""`/`"  "`), inconsistente
   com a normalização já aplicada a email.** Não explorável para IDOR (um `sub` real do Keycloak nunca é
   vazio, então o pior caso é negar acesso ao dono legítimo, nunca conceder a um impostor), mas era uma
   inconsistência com o padrão já estabelecido. **Corrigido:** `AlunoService.normalizarKeycloakSubjectId`
   trata `null`/string em branco como "sem vínculo" (mesmo padrão de `normalizarEmail`).
4. **[Baixo, code-reviewer] Comentário impreciso em `MatriculaRepository.findAlunoKeycloakSubjectIdByMatriculaId`**
   sobre o motivo da projeção escalar (dizia evitar `LazyInitializationException` por o resolver não ser
   `@Transactional`, mas métodos de repositório já são transacionais por padrão independente disso).
   **Corrigido:** comentário ajustado para o motivo real (evitar carregar a entidade inteira/N+1).

**Achados descartados com justificativa (não corrigidos):**
- **[Baixo, security-auditor] Camada de service sem checagem de autorização independente do
  `@PreAuthorize` do controller.** Hoje não é explorável (só os controllers chamam `MatriculaService`), e
  duplicar a checagem de posse na camada de service seria complexidade adicional para um risco puramente
  hipotético (futuro chamador interno que ignore o controller). Aceito como risco conhecido — ver seção
  "Pendências para Fase 7" da spec 006.
- **[Informativo, security-auditor] Nome da permissão `GERENCIAR` reutilizado para leitura e escrita de
  Matrícula** (em vez de uma permissão `READ` separada, como em `PERFIL`). Puramente cosmético, sem risco
  de segurança (a checagem de posse é a mesma para ambos os casos); não vale o retrabalho sob prazo.
- **[Baixo, code-reviewer] Drift entre specs/006-matricula.md e a localização real dos endpoints de
  listagem** (`GET /api/alunos/{alunoId}/matriculas`/`GET /api/turmas/{turmaId}/matriculas` foram
  implementados em `AlunoController`/`TurmaController`, não em `MatriculaController` como a seção 4.2/4.5
  da spec original descrevia) — endereçado atualizando a spec para refletir a implementação real (melhor
  aninhamento REST), não revertendo o código.

**Trade-offs aceitos:** O handler genérico de `DataIntegrityViolationException` (item 1) tem uma mensagem
menos específica que os `errorCode`s de negócio dedicados (`MATRICULA_DUPLICADA`, etc.) — aceitável porque
só dispara na janela de corrida rara; o caminho feliz continua retornando o `errorCode` específico via a
checagem em código.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado além do já mencionado nos
itens descartados acima.

---

<a id="d032"></a>
## D032 — Topic exchange para eventos de Matrícula externalizados

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário (razão já apresentada pelo próprio Pablo ao definir o
escopo desta fase — só formalizada aqui)
**Spec relacionada:** specs/007-mensageria-rabbitmq.md
**Contexto:** Definir a topologia do RabbitMQ (D002) para os eventos `MatriculaCriada`/`Confirmada`/
`Cancelada` externalizados via `spring-modulith-events-amqp`.

**Alternativas consideradas:**
- **Topic exchange**, routing key `matricula.criada`/`matricula.confirmada`/`matricula.cancelada` — permite
  bindings por padrão (`matricula.#` para consumir tudo, ou `matricula.confirmada` para um consumidor
  futuro interessado só nessa transição), sem tocar no publisher.
- **Direct exchange**, uma routing key por tipo de evento — mais simples de depurar, mas sem suporte a
  padrões; um consumidor parcial futuro exigiria saber a lista exata de routing keys.

**Decisão:** Topic exchange (`gestao.eventos`).

**Justificativa:** Extensibilidade sem reabrir o publisher — exatamente o cenário citado no PRD como
diferencial de mensageria bem desenhada, e a única fila desta fase (`notificacao.matricula`) já usa o
padrão `matricula.#` para provar o mecanismo sem precisar declarar 3 bindings individuais.

**Trade-offs aceitos:** Nenhum identificado — topic exchange não tem custo de complexidade real sobre
direct exchange no RabbitMQ.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado.

---

<a id="d033"></a>
## D033 — Retry: 3 tentativas, TTL fixo de 10s, DLQ nativa do RabbitMQ

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário
**Spec relacionada:** specs/007-mensageria-rabbitmq.md
**Contexto:** Definir quantas tentativas o consumidor tem antes de a mensagem ir para a DLQ final, e a
estratégia de espera entre tentativas (fixo vs. incremental) — pedido explícito do usuário para não deixar
como omissão.

**Alternativas consideradas:**
- **3 tentativas, TTL fixo de 10s** — simples de implementar (uma única fila de retry) e de demonstrar/
  testar sem esperas longas.
- **5 tentativas, TTL incremental (10s/30s/60s)** — mais realista para produção (dá mais tempo para uma
  falha transitória se resolver sozinha), mas exige uma fila de retry por nível de TTL e testes mais lentos.

**Decisão:** 3 tentativas, TTL fixo de 10s.

**Justificativa:** Proporcional ao prazo e ao escopo do desafio (não há SLA de produção real a atender);
backoff incremental e comparação de estratégias de retry ficam fora de escopo aqui, mesmo espírito de D024
(Fase 3) — simples e correto agora, aprofundamento é Fase 7 se necessário.

**Trade-offs aceitos:** Sob uma falha transitória mais longa que ~30s (3×10s) no total, a mensagem esgota as
tentativas e vai para a DLQ mesmo que a causa raiz já tivesse se resolvido — aceitável para o escopo deste
desafio; reprocessamento manual da DLQ é sempre possível (RabbitMQ Management UI).

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se este projeto evoluísse para produção real,
revisitar com backoff incremental e métricas reais de duração de falha transitória.

---

<a id="d034"></a>
## D034 — Propagação de trace ID via suporte nativo do Spring Boot/AMQP (Observation)

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA revisada pelo usuário
**Spec relacionada:** specs/007-mensageria-rabbitmq.md
**Contexto:** Sem o trace ID da requisição HTTP original propagado até o consumidor, uma mensagem na DLQ
não é rastreável até a requisição que a gerou — enfraquece a resposta a "como você trata falha de
mensageria" (PRD/entrevista).

**Alternativas consideradas:**
- **Suporte nativo do Spring Boot 3/Spring AMQP via Observation**
  (`spring.rabbitmq.template.observation-enabled=true` + `spring.rabbitmq.listener.observation-enabled=true`)
  — reaproveita a mesma infraestrutura de Micrometer Tracing já configurada na spec 002, sem código manual.
- **`MessagePostProcessor` manual lendo o `Tracer` atual** — controle total, garantido de funcionar
  independente de auto-configuração, mas código extra para manter, reimplementando algo que o Spring Boot 3
  já tenta cobrir de fábrica.

**Decisão:** Suporte nativo via Observation. **Revisto após verificação empírica (ver "Atualização" abaixo)
— NÃO é o mecanismo final implementado.**

**Justificativa:** Menos código para manter, mesmo padrão de instrumentação já usado no resto do projeto
(HTTP, JDBC). Precisa de verificação empírica durante a implementação especificamente para o caminho de
externalização do Modulith (`@Externalized`) — não há garantia a priori de que o publish passa pelo mesmo
`RabbitTemplate` instrumentado que um `convertAndSend` direto; se não propagar sozinho, cai para a opção
manual como fallback documentado, não como retrabalho silencioso.

**Trade-offs aceitos:** Nenhum identificado além do risco de verificação mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se a verificação empírica mostrar que o
caminho de externalização do Modulith não propaga o trace automaticamente, implementar o
`MessagePostProcessor` manual como complemento (não substituição) — registrar esse ajuste aqui mesmo, sem
nova entrada, se acontecer.

**Atualização (verificação empírica, mesma data):** confirmado que a opção nativa NÃO propaga — rodando a
aplicação real (Postgres/Redis/RabbitMQ/Keycloak + stack de observabilidade via compose), uma linha de log
disparada por uma requisição HTTP mostra `traceId`/`spanId` normalmente, mas o log do consumidor
(`MatriculaNotificacaoListener`) não, mesmo com `spring.rabbitmq.template/listener.observation-enabled=true`.

O fallback originalmente cogitado (`MessagePostProcessor` manual no `RabbitTemplate`, lendo
`tracer.currentSpan()` no momento do publish) **também foi tentado e também falhou** — não é só a
autoconfiguração de Observation que não cobre esse caminho; qualquer captura feita no momento em que o
Modulith efetivamente publica para o RabbitMQ falha, porque essa publicação roda de forma assíncrona numa
thread própria (ex: `task-1`), fora do escopo do `ThreadLocal` de span do Micrometer da requisição HTTP
original. Confirmado via log de diagnóstico temporário: `tracer.currentSpan()` retornava `null` nessa
thread, tanto na chamada nativa quanto no customizer manual.

**Mecanismo final implementado:** captura do `traceId` **na própria thread da requisição HTTP**, dentro de
`MatriculaService` (método privado `traceIdAtual()`, injeta `Tracer` via construtor), no exato ponto de
cada `eventPublisher.publishEvent(...)` — ou seja, antes de qualquer despacho assíncrono do Modulith
acontecer. O valor viaja como um campo `traceId` no próprio payload do evento (`MatriculaCriada`,
`MatriculaConfirmada`, `MatriculaCancelada` — ao lado do `eventId` já existente, D035), não mais como header
AMQP. O consumidor (`MatriculaNotificacaoListener`) lê esse campo genericamente via `ObjectMapper.readTree`
(sem amarrar a um tipo de evento específico, já que a routing key só é decidida depois) e coloca em
`MDC.put("traceId", ...)` antes de despachar para `processarCriada`/`processarConfirmada`/`processarCancelada`,
removendo do MDC num bloco `finally`. `MensageriaTracingConfig` (o `RabbitTemplateCustomizer` da tentativa
anterior) foi removido do código — não tinha mais função, e mantê-lo seria código morto que não funciona.

---

<a id="d035"></a>
## D035 — Decisões menores agrupadas: eventId para idempotência, topologia no pacote `config`, observabilidade da DLQ

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado durante o planejamento (grupado por baixo risco,
mesmo padrão de D022/D028/D030)
**Spec relacionada:** specs/007-mensageria-rabbitmq.md
**Contexto:** Conjunto de decisões técnicas menores identificadas ao planejar esta fase, agrupadas para não
inflar o log:

- **`eventId` (UUID) adicionado aos 3 records de evento** (`MatriculaCriada`/`Confirmada`/`Cancelada`,
  módulo `academico`) — gerado na criação do evento (em `MatriculaService`), viaja no payload JSON
  serializado pelo Modulith até o consumidor. Necessário porque nenhum dos 3 records tinha um identificador
  estável por instância de evento antes desta fase — sem isso, a tabela `evento_processado` (dedupe de
  idempotência, item explícito do escopo) não tem contra o que comparar. Segue o padrão já referenciado
  como exemplo no `SKILL.md` de decision-log.
- **Topologia do RabbitMQ (exchange/filas/bindings) declarada no pacote `config`**
  (`br.com.desafio.tecnico.gestao.config`), não dentro de `academico` nem de `notificacao` — é contrato
  compartilhado entre o módulo publicador (via `@Externalized`) e o módulo consumidor (`@RabbitListener`),
  mesmo padrão já usado para `OpenApiConfig` (configuração cross-cutting, não pertence a nenhum módulo de
  domínio específico).
- **Observabilidade da DLQ:** sem endpoint novo — RabbitMQ Management UI (já exposta na Fase 1, D008) é
  suficiente para inspecionar a fila `notificacao.matricula.dlq` manualmente; complementado por um log
  estruturado (nível WARN) no momento em que o consumidor decide rotear uma mensagem para a DLQ (após
  esgotar as 3 tentativas, D033), e um contador Micrometer (`mensageria.dlq.eventos`) para aparecer no
  Prometheus/Grafana já configurados.
- **Um único consumidor de referência** (`notificacao.matricula`, bound com `matricula.#`) — conforme já
  delimitado no escopo desta fase pelo próprio Pablo ("um consumidor de referência é suficiente para provar
  o mecanismo"), não uma decisão nova.
- **`spring.rabbitmq.listener.simple.default-requeue-rejected=false`** — obrigatório para o design de retry
  de D033 funcionar: sem isso, uma exceção no listener faz o RabbitMQ devolver a mensagem para o início da
  MESMA fila (loop apertado de falha imediata), nunca chegando à fila de espera com TTL. Não é uma decisão
  com alternativa razoável — é um requisito técnico do desenho já decidido, registrado aqui para não passar
  despercebido.

**Decisão:** Conforme detalhado acima.

**Justificativa:** Escolhas de baixo risco, consequência direta de decisões já tomadas (D032/D033) ou
padrões já estabelecidos no projeto (D008 para observabilidade, `OpenApiConfig` para configuração
cross-cutting).

**Trade-offs aceitos:** Nenhum identificado além do já mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:**

- **Rede do compose sem autenticação adicional entre serviços:** qualquer container na mesma rede Docker
  alcança o RabbitMQ com as credenciais do `.env` (não há mTLS nem segmentação de rede) — aceitável no
  escopo deste desafio (ambiente de desenvolvimento local), mas exigiria mTLS entre serviços ou rede
  segmentada em produção. Documentado também em `specs/007-mensageria-rabbitmq.md` §8 e `README.md`
  ("Mensageria assíncrona"), citado aqui explicitamente (achado de security review: a citação original no
  README apontava para este D035, mas o risco em si só estava descrito na spec — corrigido para que
  `docs/DECISIONS.md`, a fonte primária granular de decisões, também contenha o texto do risco, não só a
  referência a ele).
- Consequência prática do ponto acima: o console de management do RabbitMQ (porta 15672) concede
  administração completa do broker (criar/apagar filas, publicar/reenviar mensagens arbitrárias, não só
  leitura), não apenas inspeção. Achado de security review: essa porta estava publicada sem restrição de
  interface no `compose.yaml`, inconsistente com todo outro console admin do mesmo arquivo (Keycloak,
  Grafana, Prometheus, Jaeger, Loki — todos restritos a `127.0.0.1`). Corrigido para seguir o mesmo padrão
  (`127.0.0.1:${RABBITMQ_MANAGEMENT_PORT:-15672}:15672`).
- Consequência adicional (baixo risco, não corrigida - aceita): como qualquer publisher autenticado no
  vhost pode setar um header `x-death` arbitrário numa mensagem publicada diretamente (via Management UI ou
  código), é possível forjar uma mensagem que pareça ter esgotado as 3 tentativas e cai direto na DLQ sem
  passar pelo fluxo de retry real. Impacto limitado à integridade de métricas/observabilidade (não há perda
  de dado, escalonamento de privilégio ou execução de código) e já está coberto pelo risco acima (quem
  alcança o broker já pode publicar/purgar/reenviar mensagens arbitrárias de qualquer forma) - não introduz
  uma nova superfície.
- **Duplicação de string literals (exchange/routing keys) entre publicador e consumidor:** achado de code
  review - `@Externalized` nos records de evento (`academico`) e o `switch` do consumidor
  (`MatriculaNotificacaoListener`) referenciavam `"gestao.eventos"`/`"matricula.criada"` etc. como literais
  soltos, sem fonte única. Corrigido: `MensageriaConfig` ganhou constantes
  `ROUTING_KEY_MATRICULA_CRIADA/CONFIRMADA/CANCELADA`, e os 3 records + o `switch` do consumidor passaram a
  referenciá-las (`@Externalized` aceita expressão de constante em tempo de compilação, então
  `MensageriaConfig.EXCHANGE_EVENTOS + "::" + MensageriaConfig.ROUTING_KEY_MATRICULA_CRIADA` é válido).

---

<a id="d036"></a>
## D036 — Biblioteca de autenticação Keycloak no frontend

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado durante o planejamento
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** O frontend Angular precisa autenticar o usuário contra o Keycloak (realm `gestao`, client
público `gestao-frontend`) antes de consumir a API. Era preciso escolher a biblioteca de integração.

**Alternativas consideradas:**
- **`angular-oauth2-oidc`** — biblioteca OIDC genérica, não específica do Keycloak; mais flexível para
  trocar de provedor de identidade no futuro, mas exige montar manualmente convenções que o adapter
  oficial do Keycloak já resolve prontas.
- **`keycloak-js` + `keycloak-angular`** — adapter oficial do Keycloak, já resolve Authorization Code +
  PKCE, refresh de token e leitura de `realm_access.roles` com pouca configuração.

**Decisão:** `keycloak-js` + `keycloak-angular`.

**Justificativa:** Não há requisito de trocar de provedor de identidade neste desafio; o adapter oficial
reduz código de integração e é o caminho mais direto para PKCE + leitura de papéis RBAC do token.

**Trade-offs aceitos:** Acoplamento à API do Keycloak — trocar de provedor de identidade no futuro
exigiria reescrever a camada de autenticação do frontend.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado no escopo atual.

**Implementação:** token mantido em memória (não `localStorage`/`sessionStorage`), mitigação de XSS —
confirmado pelo `security-auditor` (nenhum uso de storage persistente para o token em todo `frontend/src`).

---

<a id="d037"></a>
## D037 — Gestão de estado no frontend: services + RxJS/Signals, sem NgRx

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado durante o planejamento
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** O frontend tem 5 features (Curso, Disciplina, Turma, Aluno, Matrícula) com estado local
simples (listas, formulário atual, loading/erro) — sem estado compartilhado complexo entre features.

**Alternativas consideradas:**
- **NgRx** — store centralizada, previsível e testável para estado complexo/compartilhado, mas adiciona
  boilerplate (actions/reducers/effects/selectors) desproporcional a um estado por feature simples.
- **Services + RxJS/Signals** — cada feature mantém seu próprio estado via `signal`, exposto pelo
  service; sem dependência nova, menos código para o mesmo resultado neste escopo.

**Decisão:** Services + RxJS/Signals, sem NgRx.

**Justificativa:** Nenhuma feature desta fase precisa de estado verdadeiramente compartilhado entre
telas distintas — o custo de configurar e manter uma store NgRx não se paga no tamanho atual da
aplicação.

**Trade-offs aceitos:** Se o frontend crescer e passar a precisar de estado compartilhado entre muitas
features (ex: cache global de Turmas), a lógica de sincronização entre services precisaria ser montada
manualmente em vez de vir de uma store centralizada.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Revisitar NgRx (ou similar) se o número de
features/estado compartilhado crescer muito além do escopo atual.

---

<a id="d038"></a>
## D038 — Estrutura de pastas do frontend por feature

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado durante o planejamento
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** Era preciso decidir a organização de pastas de `frontend/src/app` — por feature de domínio
ou espelhando os módulos Modulith do backend (`academico`, `notificacao`, `security`).

**Alternativas consideradas:**
- **Espelhando os módulos Modulith do backend** — reforça visualmente a simetria com o backend, mas o
  backend agrupa por contexto delimitado (bounded context), enquanto o frontend agrupa naturalmente por
  tela/recurso (Curso, Disciplina, Turma, Aluno, Matrícula não mapeiam 1:1 para `academico`/`notificacao`).
- **Por feature** (`core/`, `shared/`, `features/{curso,disciplina,turma,aluno,matricula}/`) — convenção
  padrão do ecossistema Angular, alinhada à granularidade real das telas.

**Decisão:** Por feature.

**Justificativa:** A granularidade de tela (Curso/Disciplina/Turma/Aluno/Matrícula) é mais natural para
organizar componentes/services do que a fronteira de módulo do backend, que representa outro nível de
abstração (contexto delimitado, não tela).

**Trade-offs aceitos:** Nenhum identificado além do já mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado no escopo atual.

---

<a id="d039"></a>
## D039 — Versão do Angular: 20

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, apresentado e não contestado durante o planejamento
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** Era preciso escolher a versão do Angular para o novo projeto `frontend/`.

**Alternativas consideradas:**
- **Angular 18 (LTS)** — suporte de longo prazo, mais previsível para um projeto de produção real.
- **Angular 20** — versão mais recente na época da implementação, standalone components como padrão
  (sem NgModules), sinaliza domínio de recursos atuais do framework — relevante para uma entrevista
  técnica.

**Decisão:** Angular 20.

**Justificativa:** Este é um desafio técnico avaliado por uma entrevista, não uma aplicação de produção
de longo prazo — usar a versão mais recente comunica domínio do estado atual do framework sem custo real,
já que não há uma base de código legada para migrar.

**Trade-offs aceitos:** Menor tempo de maturidade em produção da versão 20 comparado a uma LTS — sem
relevância prática no escopo deste desafio.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se este projeto evoluísse para produção real
de longo prazo, reavaliar a política de versão (LTS vs. mais recente).

---

<a id="d040"></a>
## D040 — "Meu Perfil" do ALUNO via claims do token (superada por D041)

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, decidido durante o planejamento
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** A tela "Meu Perfil" do ALUNO precisa mostrar dados do próprio Aluno, mas nenhum endpoint de
autoleitura existia na API antes desta fase — só CRUD staff-only (`GET /api/alunos/{id}`, restrito a
SECRETARIA/ADMIN).

**Alternativas consideradas:**
- **Claims do token JWT** — usar só os campos já presentes no token do Keycloak (nome, e-mail) sem
  nenhuma chamada à API; não expõe dados de domínio (ex: data de cadastro) que só existem na tabela
  `aluno`.
- **Cortar "Meu Perfil" do escopo** — evita o problema, mas remove uma tela explicitamente pedida pelo
  PRD (ALUNO consultar seus próprios dados).
- **Novo endpoint no backend** (`GET /api/alunos/me`) — ver D041.

**Decisão (intermediária, depois superada):** Claims do token, como primeira tentativa de manter o
escopo desta fase restrito ao frontend, sem tocar no backend.

**Justificativa:** Evitar alterar a API já estabilizada das Fases 2-4 por uma tela nova, se os dados do
token bastassem.

**Trade-offs aceitos:** Claims do token não incluem dados de domínio (ex: data de cadastro do Aluno na
tabela `aluno`, distinta da data de criação do usuário no Keycloak) — a tela ficaria incompleta.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Superada por D041 ainda durante esta mesma
fase, ao ficar claro que o ALUNO também precisa do próprio `alunoId` (não só do nome/e-mail) para poder
se matricular — ver D041.

---

<a id="d041"></a>
## D041 — Endpoint `GET /api/alunos/me` para o ALUNO descobrir o próprio `alunoId`

**Data:** 2026-07-14
**Origem:** 🤝 Sugestão da IA, revisada por mim (achado durante a implementação, não previsto no
planejamento original — ver D040)
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** Ao implementar o fluxo de matrícula do ALUNO, ficou claro que as claims do token (D040) não
bastam: o ALUNO precisa do próprio `alunoId` (chave da tabela `aluno`, distinta do `sub` do JWT) para
poder chamar `POST /api/matriculas`. Nenhum endpoint da API resolvia "qual Aluno é este usuário logado".

**Alternativas consideradas:**
- **Manter só claims do token (D040)** — insuficiente, bloqueia o próprio fluxo de matrícula do ALUNO.
- **Cortar matrícula do ALUNO do escopo desta fase** — evita mudar o backend, mas remove o fluxo mais
  citado do PRD (ALUNO se matricula pela própria UI).
- **Adicionar `GET /api/alunos/me`** — endpoint aditivo mínimo em `academico`, resolve o Aluno a partir
  do `sub` do JWT (via o vínculo Aluno↔Keycloak já modelado em D022/D030), sem parâmetro manipulável pelo
  cliente.

**Decisão:** Adicionar `GET /api/alunos/me`.

**Justificativa:** É o único caminho que preserva o fluxo de matrícula do ALUNO (critério mais citado do
PRD) sem inventar um mecanismo alternativo de descoberta de identidade no frontend.

**Implementação:** endpoint resolve o Aluno unicamente a partir do `sub` do JWT autenticado — nunca
aceita um `id`/parâmetro externo, então não pode retornar o registro de outro Aluno. Sobrescreve o
`@PreAuthorize` de classe do controller (staff-only) só para este método.

**Trade-offs aceitos:** Mais um endpoint na API estabilizada das Fases 2-4, fora do plano original desta
fase de frontend.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado — endpoint coberto por
teste de integração dedicado (`AlunoControllerIntegrationTest`, achado de code review, ver seção 10 de
specs/008), incluindo teste de regressão de que o override não vaza para os demais endpoints do
controller.

---

<a id="d042"></a>
## D042 — CORS restrito à origem do frontend

**Data:** 2026-07-14
**Origem:** 🤖 Default da IA, decidido durante a implementação sem pausar para perguntar (requisito
técnico descoberto ao validar a spec 008 — sem alternativa razoável)
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** Ao validar o frontend (`ng serve`, origem `http://localhost:4200`) contra a API
(`http://localhost:8080`), nenhuma chamada funcionava — o backend nunca precisou de CORS antes desta
fase, porque não existia um consumidor HTTP de outra origem.

**Alternativas consideradas:**
- **Nenhuma outra alternativa razoável** — CORS restrito à origem real do frontend é o único
  comportamento correto para um backend que aceita chamadas de um frontend em origem diferente; wildcard
  (`*`) ou reflexão do header `Origin` foram descartados por exporem a API a qualquer origem.

**Decisão:** `CorsConfigurationSource` no `SecurityConfig`, restrito à origem configurada em
`app.frontend.origin` (`APP_FRONTEND_ORIGIN`, default `http://localhost:4200`).

**Justificativa:** É um requisito técnico, não uma escolha entre alternativas de mesmo mérito — sem CORS
configurado, o frontend simplesmente não funciona; com wildcard, a API ficaria aberta a qualquer origem.

**Trade-offs aceitos:** Nenhum identificado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o frontend for servido de uma origem
diferente em outro ambiente (ex: produção), `APP_FRONTEND_ORIGIN` precisa ser ajustada nesse ambiente.

**Refinamento pós-code-review (2026-07-14):** a propriedade não usava a indireção `${ENV_VAR:...}` já
padrão no resto do `application.properties` — corrigido para `${APP_FRONTEND_ORIGIN:http://localhost:4200}`,
documentado em `.env.example`.

---

<a id="d043"></a>
## D043 — Sem abstração `CrudService<T>` genérica no frontend

**Data:** 2026-07-14
**Origem:** 🧑 Decisão do Pablo (achado de code review, registrado retroativamente)
**Spec relacionada:** specs/008-frontend-angular.md
**Contexto:** Os services/formulários de Curso, Disciplina, Turma e Aluno repetem a mesma estrutura
(listar, buscar por id, criar, editar, excluir) — o `code-reviewer` apontou a duplicação e perguntou se
valia a pena extrair uma abstração genérica.

**Alternativas consideradas:**
- **`CrudService<T>` genérico** — reduziria a repetição entre os 4 services, mas cada recurso tem
  particularidades (ex: `Turma` depende do par `curso_id`/`disciplina_id`; `Aluno` tem a leitura extra de
  `/me`, D041) que forçariam a abstração a virar genérica-com-excecões rapidamente.
- **Sem abstração genérica** — manter os 4 services explícitos, aceitando a repetição.

**Decisão (do Pablo, contrariando o default sugerido pela IA):** sem abstração genérica.

**Justificativa (do Pablo):** Dado o orçamento de tempo restante e o tamanho real do problema (4
recursos, cada um já com uma particularidade que quebraria uma abstração genérica), o custo de desenhar e
manter uma abstração correta supera o ganho de remover a duplicação atual.

**Trade-offs aceitos:** Repetição de código entre os 4 services/formulários — aceitável no volume atual;
revisitar se mais recursos CRUD forem adicionados no futuro.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o número de recursos CRUD crescer muito,
reavaliar uma abstração genérica com mais tempo disponível.

---

<a id="d044"></a>
## D044 — Direção visual "Institucional acadêmico" para o redesign do frontend

**Data:** 2026-07-12
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/009-design-system-sidebar.md
**Contexto:** O resultado visual do frontend entregue na spec 008 (identidade navy/laranja, tabelas
densas) foi avaliado pelo próprio Pablo como pobre — sem hierarquia visual, ícones ou tratamento de
estados vazio/loading. Era preciso escolher uma nova direção visual antes de qualquer código, usando o
subagent `ui-designer` (recém-instalado) para conduzir a execução.

**Alternativas consideradas** (apresentadas com mockup comparativo, mesma tela real de Turmas com sidebar):
- **SaaS/ERP neutro** — paleta quase monocromática (cinza-azulado) com um único accent cobalto, tipografia
  de sistema + monoespaçada para dados, ícones lineares finos. Mood "ferramenta de trabalho", mais próximo
  de Linear/Notion. Era a recomendação inicial da IA.
- **Institucional acadêmico** — sidebar em navy profundo, dourado restrito ao estado ativo/ênfase, títulos
  em serifada clássica (Palatino/Georgia), fundo marfim. Mood de credibilidade institucional (diploma/
  selo universitário), mais alinhado ao domínio real do sistema (gestão acadêmica).
- **EdTech vibrante** — paleta mais colorida e cantos arredondados, mood de produto de consumo (descartada
  já na fase de brainstorming, antes do mockup, por risco de incoerência com o tom ERP/institucional
  esperado das telas de SECRETARIA/ADMIN).

**Decisão (do Pablo, contrariando a recomendação inicial da IA):** Institucional acadêmico (navy + dourado
+ serifada), depois de ver os dois mockups lado a lado.

**Justificativa (do Pablo):** A direção institucional comunica melhor o domínio real do produto (gestão
acadêmica) do que a alternativa neutra de SaaS genérico — decisão tomada visualmente, comparando os dois
mockups, não só pela descrição textual das opções.

**Tokens resultantes:** `--navy:#16264A`, `--navy-2:#223768`, `--gold:#A9812F` (accent, restrito a
estado ativo/ênfase — nunca reutilizado para semântica de erro/aviso), `--bg:#FAFAF7`, `--ink:#1C2230`,
`--muted:#6B6456`, mais os semânticos `--ok`/`--warn`/`--danger` (deliberadamente distintos do dourado,
para não sobrecarregar o accent com significado de erro/aviso — refinamento sobre o mockup inicial, que
reaproveitava o dourado como cor de aviso). Serifada (`"Iowan Old Style", "Palatino Linotype", Palatino,
Georgia, "Times New Roman", serif`) só em títulos de página/seção e na marca — cabeçalho de tabela
continua em sans maiúsculo, não itálico serifado como no mockup, por legibilidade em alta densidade de
dados.

**Trade-offs aceitos:** Reabre uma decisão de escopo já registrada na spec 008 ("polimento visual fora de
escopo por prazo") — investimento de tempo extra que a spec original não previa.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Nenhum identificado — decisão de
apresentação, reversível se uma direção diferente for preferida mais tarde (os tokens estão concentrados
em `styles.css`, não espalhados pelo código).

---

<a id="d045"></a>
## D045 — Escopo e integração técnica da administração de usuários/papéis

**Data:** 2026-07-12
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/010-administracao-usuarios-papeis.md
**Contexto:** O Pablo pediu uma tela para ADMIN gerenciar usuários e permissões. Era preciso fechar, antes
de implementar: até onde essa tela vai (só papel, ou também ciclo de vida do usuário), como o backend fala
com o Keycloak, e quem pode acessá-la — três decisões tomadas juntas na mesma rodada de brainstorming,
por estarem diretamente interligadas (o escopo determina a superfície de integração necessária).

**Alternativas consideradas:**
- *Escopo:* (a) só reatribuir papel de usuários existentes; (b) (a) + habilitar/desabilitar usuário; (c)
  CRUD completo (+ criar usuário, resetar senha) — escopo (c) se aproxima de reimplementar uma fração do
  próprio console admin do Keycloak dentro da aplicação.
- *Integração com Keycloak:* (a) `keycloak-admin-client` (SDK oficial Java, métodos tipados); (b) chamadas
  REST manuais (`RestClient` do Spring, parsing manual de JSON, sem dependência nova); (c) frontend
  chamando a Admin API do Keycloak diretamente — descartada de saída por exigir um token com privilégio
  administrativo do Keycloak no browser, superfície de risco desproporcional ao ganho.
- *Acesso:* (a) só ADMIN; (b) SECRETARIA e ADMIN (mantém a equivalência atual dos dois papéis, D011).

**Decisão:** Escopo (a) só reatribuir papel; integração (a) `keycloak-admin-client`; acesso (a) só ADMIN.

**Justificativa (do Pablo):** Escopo mínimo que já entrega valor real (gerenciar quem tem qual papel) sem
reconstruir o console do Keycloak. `keycloak-admin-client` troca uma dependência nova por bem menos código
de integração/parsing manual, reduzindo risco de erro numa superfície sensível (concessão de privilégio).
Restringir a ADMIN é a primeira regra real que diferencia esse papel de SECRETARIA no backend — fecha o
ponto que D011 já deixava aberto ("revisitar se surgir uma regra real que os diferencie").

**Trade-offs aceitos:** Mais uma dependência Maven (`keycloak-admin-client`); escopo deliberadamente menor
do que "gestão completa de usuários" — criar usuário/resetar senha continuam exigindo o console admin do
Keycloak diretamente.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se surgir necessidade real de criar usuários
pela própria aplicação (não só reatribuir papel), revisitar o escopo (b)/(c) acima.

---

<a id="d046"></a>
## D046 — Decisões menores agrupadas: módulo `administracao`, seção Administração da sidebar adiada, service account do `gestao-backend`

**Data:** 2026-07-12
**Origem:** 🤖 Default da IA, apresentado e não contestado durante o planejamento (grupado por baixo risco,
mesmo padrão de D022/D030/D035)
**Spec relacionada:** specs/009-design-system-sidebar.md, specs/010-administracao-usuarios-papeis.md
**Contexto:** Conjunto de decisões técnicas menores identificadas ao planejar as specs 009/010, agrupadas
para não inflar o log:

- **Novo módulo Modulith `administracao`** (em vez de colocar o controller/service de gestão de
  usuários dentro de `security`) — mantém `security` focado em configuração transversal de
  autenticação/autorização (`SecurityConfig`, `PermissionEvaluator`), em vez de acumular também a lógica
  de negócio de gestão de usuários do Keycloak, que é um contexto de aplicação distinto.
- **Sidebar (spec 009) já cria a seção "Administração"** antes da tela em si existir (spec 010) — a seção
  só fica visível para ADMIN e pode ficar vazia/apontar para uma rota ainda inexistente entre a conclusão
  da spec 009 e da spec 010, dependendo da ordem de implementação escolhida na execução.

  **Refinamento durante a execução do plano da spec 009 (2026-07-12):** decisão revertida — a seção
  "Administração" **não** foi criada na Task 2 do plano (`docs/superpowers/plans/2026-07-12-design-system-sidebar.md`).
  Um `nav-item`/`nav-section` sem rota real por trás (link morto) foi avaliado como pior do que simplesmente
  adiar a seção inteira; a spec 010 agora cria a seção e o item juntos, como uma unidade, em vez de a
  spec 009 antecipar um slot vazio. Achado pelo `task-reviewer` durante a revisão da Task 2 (divergência
  entre o plano e esta entrada/spec 009 §6), confirmado pelo Pablo.
- **`serviceAccountsEnabled: true` no client `gestao-backend`**, com client roles `view-users`/
  `manage-users`/`query-users` de `realm-management` atribuídas ao seu service account — requisito técnico
  da integração via `client_credentials` (D045), não uma escolha entre alternativas: sem isso, a chamada à
  Admin API do Keycloak nunca autentica. Mesmo padrão de "requisito técnico sem alternativa razoável" já
  usado em D042 (CORS).

**Decisão:** Conforme detalhado acima.

**Justificativa:** Escolhas de baixo risco, consequência direta da decisão principal já tomada em D045 ou
de padrões já estabelecidos no projeto (separação de módulo por contexto, D001/D005).

**Trade-offs aceitos:** Nenhum identificado além do já mencionado.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se a sidebar da spec 009 for implementada
antes da spec 010, decidir na execução se a seção "Administração" fica oculta até a rota existir ou
aparece com um placeholder — detalhe de sequenciamento, não uma decisão de arquitetura.

**Refinamento (2026-07-13) — client role `view-realm` faltante bloqueava `listarUsuarios()`:** ao rodar o
e2e real (`e2e/administracao-papel-flow.sh`, Task 6 do plano de execução) contra a stack completa pela
primeira vez, `GET /api/admin/usuarios` retornava 500 — causa raiz: `jakarta.ws.rs.ForbiddenException`
(403) dentro de `AdministracaoUsuarioService`, na chamada `realmResource.roles().get(papel)
.getUserMembers()`. O role mapping original desta entrada (`view-users`/`manage-users`/`query-users`)
cobre operações sobre *usuários*, mas o endpoint de membros de uma *role* (`/roles/{role-name}/users`) é
avaliado pelo Keycloak como operação de *realm* (`RoleContainerResource`), exigindo `view-realm` — não
apenas `view-users` — mesmo quando a chamada é, na prática, "sobre usuários" do ponto de vista do nosso
domínio. Confirmado empiricamente: consultando o role mapping do service account via Admin API (token de
admin mestre, leitura), faltava `view-realm`; concedendo-o ao vivo (autorizado explicitamente pelo Pablo
antes da mudança, por ser uma escalação de permissão) o e2e passou integralmente nas 5 etapas. Corrigido
adicionando `view-realm` à lista de `clientRoles` do service account em
`docker/keycloak/import/gestao-realm.json`. Classificado como correção de uma lacuna na config original
desta entrada (D046), não uma decisão de design nova — não há alternativa razoável para "como conceder
list-membros-de-role", é um requisito técnico da Admin API do Keycloak, mesmo padrão de "requisito técnico
sem alternativa" já usado para `serviceAccountsEnabled` acima.

## D047 — Decisões do tema Keycloakify para o login (escopo, entrega, localização, estratégia de reskin)

**Data:** 2026-07-13
**Origem:** Mista — ver cada ponto abaixo
**Spec relacionada:** specs/011-tema-keycloakify-login.md
**Contexto:** Ao planejar a spec 011 (tema Keycloakify aplicando a identidade "Institucional acadêmico" da
spec 009 ao login do Keycloak), quatro decisões técnicas não-triviais surgiram. Apresentadas ao Pablo via
perguntas de múltipla escolha antes de escrever a spec:

- **Escopo de páginas do tema — somente login.** Alternativas: somente login vs. login+account console vs.
  reskin completo (login+account+email+admin). Escolhido somente login porque é a única superfície do
  Keycloak que o usuário final efetivamente vê hoje — registro está desabilitado no realm
  (`registrationAllowed: false`) e "Meu Perfil" já é resolvido dentro do Angular, não pelo account console
  do Keycloak. 🤖 **Default da IA, aceito sem alteração** (Pablo escolheu exatamente a opção apresentada
  como mais coerente com o que já existe, sem contestar).
- **Estratégia de entrega do tema ao container — JAR provider via volume.** Alternativas: diretório de
  tema sem JAR montado direto em `/opt/keycloak/themes` vs. JAR provider montado em
  `/opt/keycloak/providers` vs. imagem Docker customizada que builda e empacota o tema. 🤝 **Sugestão da
  IA, revisada por Pablo** — a proposta inicial (menor atrito) era o diretório sem JAR; Pablo optou pelo
  JAR provider em vez disso.
- **Local do projeto no repositório — `keycloak-theme/` na raiz.** Alternativas: novo diretório na raiz vs.
  dentro de `frontend/`. Escolhido a raiz porque o Keycloakify exige toolchain React+Vite própria,
  incompatível com o workspace Angular CLI de `frontend/`. 🤖 **Default da IA, aceito sem alteração.**
- **Estratégia de reskin — CSS/tokens sobre o `Template` default do Keycloakify**, sem `eject` de páginas
  individuais em React customizado. Decisão levantada e justificada pela IA (não colocada em pergunta
  separada ao Pablo, por ser de baixo risco e reversível) e aceita sem alteração ao validar o design da
  spec 011. 🤖 **Default da IA, aceito sem alteração.**

**Decisão:** Conforme detalhado em cada ponto acima.

**Justificativa:** Escopo mínimo (só login) evita reskin de superfícies do Keycloak que a aplicação não
usa; JAR provider via volume (decisão do Pablo) mantém o padrão de infra-as-código do projeto sem exigir
imagem Docker customizada; localização em `keycloak-theme/` evita misturar dois toolchains de frontend no
mesmo diretório; reskin via CSS/tokens sobre o `Template` padrão é suficiente para o objetivo (identidade
visual) sem assumir a manutenção de páginas de autenticação customizadas que o PRD não pede.

**Trade-offs aceitos:** JAR provider via volume exige rebuild do tema (`npm run build-keycloak-theme`)
antes de cada `docker compose up` que dependa de uma mudança nova no tema — não há watch/hot-reload contra
o Keycloak real (o `npm run dev` do Keycloakify cobre isso via mock local, mas é um ciclo de verificação
separado do compose). Reskin via `Template` default (em vez de páginas ejetadas) limita customizações mais
profundas de layout por página, caso surjam no futuro.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o projeto precisar futuramente de tema para
account console ou e-mail, essa é uma extensão natural do mesmo `keycloak-theme/`, não uma nova decisão de
localização. Se o fluxo de build/volume se mostrar frágil (ex: nome de artefato mudando entre versões do
Keycloakify), reconsiderar imagem Docker customizada como abordagem mais determinística.

## D048 — `AdministracaoUsuarioService.listarUsuarios()` resolve papel via membros de role (O(3)), não N+1 por usuário

**Data:** 2026-07-13
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/010-administracao-usuarios-papeis.md
**Contexto:** O código de referência da Task 3 do plano de execução
(`docs/superpowers/plans/2026-07-12-administracao-usuarios-papeis.md`) resolvia o papel de cada usuário
listado com uma chamada adicional à Admin API do Keycloak por usuário (`realmResource.users().get(id)
.roles().realmLevel().listAll()`) — ou seja, 1 chamada para listar N usuários + N chamadas para resolver
papel, um padrão de amplificação de requisições (N+1). O `task-reviewer` da Task 3 levantou isso como
achado Important, mas observou que era o próprio texto do plano (não um desvio do subagent implementador),
e sugeriu como alternativa consultar os membros das 3 roles gerenciadas diretamente
(`RolesResource.get(papel).getUserMembers()`, O(3) chamadas) e montar um mapa `userId -> papel`.
**Alternativas consideradas:**
- Manter N+1 como estava (aceitar como trade-off documentado) — mais simples de ler, mas degrada linearmente
  com o número de usuários do realm; tela de baixo tráfego hoje, mas sem necessidade real de manter o
  padrão mais caro.
- Trocar para O(3) via `getUserMembers()` por role — mais robusto à medida que a base de usuários cresce,
  mesmo número de chamadas independente de N.
**Decisão:** O(3) via `getUserMembers()` das 3 roles gerenciadas.
**Justificativa (Pablo, ao ser perguntado):** preferiu corrigir agora em vez de carregar uma pendência de
performance conhecida para depois — o custo de corrigir na hora é baixo (mesma task, subagent já mobilizado)
e evita ter que revisitar isso caso a base de usuários de teste cresça durante o resto do desenvolvimento.
**Trade-offs aceitos:** a lógica de montagem do mapa `userId -> papel` é um pouco menos direta de ler do que
"perguntar o papel deste usuário" por usuário; depende de `getUserMembers()` não paginar silenciosamente sem
os parâmetros certos (a implementação precisa confirmar que a API retorna todos os membros de cada role sem
paginação implícita, dado o volume pequeno esperado no realm local/de teste).
**Riscos conhecidos / o que revisitar se o contexto mudar:** se o Keycloak passar a impor paginação
obrigatória em `getUserMembers()` para realms grandes, revisitar a estratégia (ex: paginação explícita ou
cache local do mapeamento papel→usuários). Adicionalmente (achado da revisão final de branch,
2026-07-13): `AdministracaoUsuarioService.reatribuirPapel()` remove o(s) papel(is) atual(is) do usuário
antes de adicionar o novo (duas chamadas separadas à Admin API do Keycloak, sem transação distribuída) —
se `add()` falhar após `remove()` ter tido sucesso (ex: falha de rede intermitente entre as duas
chamadas), o usuário-alvo fica temporariamente sem nenhum papel gerenciado. Aceito como está, dado o
baixo tráfego da tela (uso administrativo pontual, não um fluxo de usuário final) — documentado aqui para
não ser redescoberto como "bug" depois; não há retry automático nem compensação implementados.

## D049 — Ferramental da prova e2e de concorrência: Playwright isolado em `e2e/playwright/`, repetição via `--repeat-each`

**Data:** 2026-07-13
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/012-concorrencia-e-testes.md
**Contexto:** A prova mais crítica desta fase é a disputa real por uma vaga entre 20 alunos, disparando 20
confirmações verdadeiramente simultâneas contra a API real. Todo o `e2e/` existente até aqui é bash + curl +
python3 — funciona bem para fluxos sequenciais, mas orquestrar 20 chamadas HTTP genuinamente simultâneas
(sem uma delas começar antes da anterior terminar de disparar) é desconfortável em bash puro; `Promise.all`
em Node é o jeito direto de garantir isso. Já entrei nesta fase com a decisão de usar Playwright
`APIRequestContext` (cliente HTTP puro do Playwright, sem simular clique de UI) já tomada — a pergunta que
restava era onde/como esse projeto Node vive no repo, dado que não existe `package.json` na raiz nem em
`e2e/` hoje.
**Alternativas consideradas:**
- Projeto Playwright dentro de `frontend/`, reaproveitando o `package.json` Angular já existente — evita
  criar mais um projeto Node, mas mistura uma dependência de teste e2e de API com o projeto de UI, fugindo
  do padrão de isolamento por responsabilidade que o repo já usa (`frontend/` e `keycloak-theme/` são cada
  um seu próprio projeto Node, sem dependências cruzadas).
- `e2e/playwright/` como projeto Node isolado, só `@playwright/test` como devDependency, sem instalar
  browsers (o `APIRequestContext` não precisa de Chromium/Firefox/WebKit — só `playwright-core`, não a
  instalação completa do pacote `playwright`) — consistente com o padrão já estabelecido de "um toolchain
  por responsabilidade".
- Repetição das 10 execuções: loop bash externo chamando `npx playwright test` 10x, vs. `--repeat-each=10`
  nativo do Playwright.
**Decisão:** `e2e/playwright/` isolado, sem download de browser; repetição via `--repeat-each=10` nativo do
Playwright, `workers: 1` (as 10 repetições rodam em série; a concorrência real de 20 já acontece dentro de
cada repetição, via `Promise.all`).
**Justificativa:** confirmei ambas as recomendações apresentadas sem alteração — o isolamento por projeto já
é o padrão do repo (nenhum motivo para quebrá-lo aqui) e `--repeat-each` dá granularidade nativa por
repetição no relatório (`(repeat:N/10)`), o que facilita apontar exatamente qual das 10 execuções falhou, se
alguma falhar, sem reinventar isso em bash.
**Trade-offs aceitos:** primeiro uso de Playwright no repo — zero precedente local para convenções de
`.gitignore`/estrutura de projeto Node com esse framework especificamente (ainda que o padrão geral de
projeto Node isolado já exista via `frontend/`/`keycloak-theme/`); mais uma stack de teste a manter, além
dos scripts bash já existentes.
**Riscos conhecidos / o que revisitar se o contexto mudar:** o comportamento de "`@playwright/test` não
baixa browser via `npm ci`" precisa ser confirmado empiricamente na implementação (não assumido cegamente)
— versões futuras do Playwright podem mudar esse comportamento de instalação; se a suposição estiver errada,
a etapa de CI precisa de um `npx playwright install` extra.

## D050 — Teardown do e2e de concorrência: curso/disciplina fixos reaproveitados (teardown completo é impossível hoje)

**Data:** 2026-07-13
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/012-concorrencia-e-testes.md
**Contexto:** O pedido original para esta fase era que o teste e2e limpasse toda a massa de dados que cria
(matrícula, turma, disciplina, curso, alunos) via chamadas à API, sempre, mesmo em falha. Ao investigar os
endpoints de exclusão existentes para desenhar esse teardown, encontrei uma restrição real do sistema:
`TurmaService.excluir()` só faz soft-delete (`turma.inativar()`, a linha nunca sai do banco); e a checagem
de FK em `CursoService.desvincularDisciplina()` (`turmaRepository.existsByCursoIdAndDisciplinaId`) **não
filtra por `ativo`** — existe justamente para não vazar um erro 500 de violação de FK composta como um 409
de negócio. Consequência: mesmo depois de "excluir" (soft-delete) uma Turma, o vínculo curso↔disciplina
nunca pode ser desfeito, e por decorrência nem a Disciplina nem o Curso associados podem ser excluídos —
não existe hard-delete de Turma em lugar nenhum da API. O teardown como pedido originalmente é, portanto,
inexecutável para curso/disciplina com o sistema como está hoje.
**Alternativas consideradas:**
- Reaproveitar um curso e uma disciplina fixos entre execuções (criação idempotente: um 409 na criação é
  tratado como "já existe, buscar id via `GET` + filtro" — o mesmo padrão que `e2e/matricula-flow.sh` já usa
  para o Aluno fixo do script), e só criar/desfazer por repetição o que realmente precisa ser único: a Turma
  (sempre nova, sempre soft-deletada ao final — funciona sem precondição) e os 20 Alunos (sempre novos,
  sempre soft-deletados ao final).
- Deixar acumular sem nenhuma limpeza — mais simples de implementar, mas suja o banco local/CI com um novo
  curso+disciplina+turma+20 alunos a cada uma das 10 repetições, toda vez que a suíte roda.
- Adicionar hard-delete real ao backend (`TurmaRepository`/serviços) só para viabilizar o teardown completo
  pedido originalmente — resolveria o pedido ao pé da letra, mas expande o escopo desta fase para tocar
  código de produção só para servir a um teste, contrariando a própria natureza desta fase (não adicionar
  funcionalidade nova).
**Decisão:** curso e disciplina fixos, reaproveitados de forma idempotente; turma e os 20 alunos sempre
novos e sempre soft-deletados via `afterEach` ao final de cada repetição.
**Justificativa (Pablo, ao ser perguntado):** confirmou a recomendação apresentada — evita tocar código de
produção só para viabilizar um teste, e é estritamente melhor que "deixar acumular sem limpeza" (reduz a
massa de dados órfã à Turma+Alunos, que já eram os únicos elementos que precisavam ser únicos por repetição
de qualquer forma).
**Trade-offs aceitos:** o curso e a disciplina fixos nunca são removidos do banco (ficam permanentemente
ativos) — aceitável, já que só existe 1 par no total, criado uma única vez; matrículas também nunca são
excluídas (não existe endpoint para isso), ficam presas a Turmas/Alunos inativos, mas isso já é o padrão do
resto do app (`findByIdAndAtivoTrue` as filtra em toda consulta normal).
**Riscos conhecidos / o que revisitar se o contexto mudar:** se o volume de execuções locais/CI crescer
muito, o número de Turmas soft-deletadas acumuladas no banco de desenvolvimento cresce sem limite (não afeta
corretude, só volume de linhas órfãs) — revisitar se isso um dia virar um problema de tamanho de banco local.

## D051 — Código de comparação com lock pessimista isolado em `src/test/java`, nunca em produção

**Data:** 2026-07-13
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/012-concorrencia-e-testes.md
**Contexto:** Esta fase pede uma comparação numérica real entre a estratégia atômica já em produção (D024) e
uma variante de lock pessimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), N=10 threads / M=1 vaga, e depois
"remover a estratégia perdedora do caminho de produção se ela não for a escolhida". Isso implica decidir,
antes de implementar, onde o código da variante pessimista vive durante a comparação.
**Alternativas consideradas:**
- Repository method `@Lock(PESSIMISTIC_WRITE)` já em `TurmaRepository` (produção), com a lógica de
  comparação (checar+incrementar+salvar) só no teste — deixaria um método de produção não utilizado até (e
  se) fosse promovido.
- Repository de teste + serviço transacional mínimo inteiramente em `src/test/java`, isolado num pacote
  próprio (`academico/concorrencia/`), importado só pelo teste de comparação via `@Import` — nada em
  `src/main/java` muda.
**Decisão:** tudo em `src/test/java`, nada em produção.
**Justificativa (Pablo, ao ser perguntado):** confirmou a recomendação — torna "remover a estratégia
perdedora do caminho de produção" um não-evento se a atômica vencer (resultado esperado, dado que já é a
estratégia validada em produção desde a spec 006): não há nada em `src/main/java` para remover. Só há
trabalho real de limpeza se a pessimista vencer inesperadamente, cenário tratado como fora do escopo desta
fase (a decisão final com os números reais da comparação será registrada numa entrada própria, na task de
fechamento desta comparação — ainda não existe nesta data).
**Trade-offs aceitos:** depende de o Spring Boot fazer component-scan da árvore inteira
`br.com.desafio.tecnico.gestao.*` sem restrição (confirmado — não há `@ComponentScan`/`@EnableJpaRepositories`
customizado em `GestaoApplication`), para que um repository declarado em `src/test/java` seja descoberto
normalmente; a implementação deve validar isso empiricamente (bean resolve no contexto do teste) antes de
confiar no desenho.
**Riscos conhecidos / o que revisitar se o contexto mudar:** se a pessimista vencer a comparação, promover o
código de `src/test/java` para produção é um follow-up manual, não automático — fora do escopo desta fase.

## D052 — Métrica `matricula.vaga.conflito` com tag `motivo`, primeira métrica com tag do projeto

**Data:** 2026-07-13
**Origem:** 🤝 Sugestão da IA, revisada por mim
**Spec relacionada:** specs/012-concorrencia-e-testes.md
**Contexto:** Esta fase pede uma métrica Micrometer visível no Grafana para conflitos/retries na confirmação
de matrícula. O único precedente do projeto (`mensageria.dlq.eventos`, `MatriculaNotificacaoListener`) é um
`Counter` simples, sem `Tags`. `MatriculaService.confirmar()` distingue dois motivos de conflito na mesma
branch (`linhasAfetadas == 0`): `VAGAS_ESGOTADAS` (vaga genuinamente esgotada) e `CONFLITO_CONCORRENCIA`
(conflito de versão não relacionado à vaga em si).
**Alternativas consideradas:**
- `matricula.vaga.conflito` sem tags, incrementado para os dois motivos igualmente — mais simples, segue o
  precedente existente à risca, mas não permite distinguir os dois motivos no Grafana.
- `matricula.vaga.conflito` com `Tags.of("motivo", errorCode)` — primeira métrica com tag do projeto, mas
  permite separar no painel quantos conflitos são vaga esgotada vs. colisão de versão não relacionada.
**Decisão:** com tag `motivo`.
**Justificativa (Pablo, ao ser perguntado):** preferiu a variante com tag em vez do default sem tags que eu
propus — quis poder distinguir os dois motivos no painel do Grafana, mesmo sendo a primeira métrica do
projeto a usar tags.
**Trade-offs aceitos:** rompe a uniformidade com o único precedente existente (`mensageria.dlq.eventos`, sem
tags) — a partir daqui, "métrica de negócio simples sem tags" deixa de ser o padrão implícito único do
projeto; cardinalidade da tag é baixa e fixa (2 valores possíveis), sem risco de explosão de séries no
Prometheus.
**Riscos conhecidos / o que revisitar se o contexto mudar:** nenhum identificado — cardinalidade fixa e
baixa, sem risco de crescimento não controlado.

## D053 — Decisão final: mantém UPDATE atômico condicional (D024), lock pessimista não substitui

**Data:** 2026-07-13
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/012-concorrencia-e-testes.md
**Contexto:** Com a prova e2e real (Task 2) e a comparação numérica JVM (Task 3) concluídas, faltava
confirmar se a estratégia atômica já em produção desde a spec 006 (D024, `UPDATE` condicional em
`TurmaRepository.consumirVaga`) devia ser mantida ou substituída pela variante de lock pessimista
implementada só para comparação (D051). Números reais coletados:
- **Task 2 (e2e, HTTP real, Keycloak real):** 20 alunos disputando a última vaga de 1 turma, 20
  confirmações verdadeiramente simultâneas (`Promise.all`), repetido 10x consecutivas duas vezes (20/20
  execuções totais) — em todas, exatamente 1×200 e 19×409/`VAGAS_ESGOTADAS`, sem exceção não tratada, sem
  timeout, sem resultado ambíguo. Teardown verificado (turma/alunos soft-deletados, curso/disciplina fixos
  preservados, D050).
- **Task 3 (JVM, N=10 threads/M=1 vaga, uma execução por estratégia):** estratégia atômica —
  `sucessos=1, conflitos=9, excecoesInesperadas=0, tempoTotalMs=32`; estratégia pessimista (`PESSIMISTIC_WRITE`,
  código isolado em `src/test/java`, D051) — `sucessos=1, conflitos=9, excecoesInesperadas=0, tempoTotalMs=33`.
  Corretude idêntica; diferença de tempo (32ms vs. 33ms) estatisticamente insignificante nessa escala e
  execução única — não é um sinal de performance confiável, apenas ruído de medição. Achado da revisão
  final de branch (2026-07-13): as duas execuções também não fazem exatamente o mesmo trabalho no banco —
  as 10 threads da estratégia atômica capturam a mesma `version` antes da corrida, então o predicado
  `WHERE ... AND version = :version` já decide o vencedor sem serialização real de lock (nove das dez
  chamadas falham por versão desatualizada, não por espera de lock); a pessimista, ao contrário, serializa
  de verdade via `PESSIMISTIC_WRITE`. Isso é fiel à produção (cada uma das 20 matrículas realmente parte da
  mesma `version` antes de disputar a vaga) e não invalida a conclusão de corretude, mas os 32ms/33ms
  comparam trabalhos diferentes no banco, não a mesma carga medida duas vezes — por isso a comparação de
  tempo já era tratada como ruído, não como decisão.
**Alternativas consideradas:**
- Manter a estratégia atômica condicional (D024) — já validada em produção desde a spec 006, sem nenhuma
  vantagem de performance demonstrada pela alternativa nessa escala, e evita manter um lock de linha aberto
  pela duração de uma transação (menor risco de cadeia de espera sob carga maior que a testada aqui).
- Trocar para lock pessimista (`PESSIMISTIC_WRITE`) — corretude equivalente nos números coletados, mas
  exigiria migrar `MatriculaService.confirmar()` de estratégia (mudança de escopo maior, fora do que esta
  fase cobre) sem nenhum ganho mensurado que justificasse o custo/risco da migração.
**Decisão:** manter a estratégia atômica condicional (D024). Nenhuma mudança em `src/main/java` — a
comparação não altera o caminho de produção.
**Justificativa (Pablo, ao ser perguntado):** confirmou a recomendação apresentada com os números reais em
mãos — a pessimista não mostrou vantagem de performance na comparação, e a atômica evita o risco adicional
de um lock de linha aberto por mais tempo sob carga maior que a testada.
**Trade-offs aceitos:** o harness de lock pessimista (`TurmaLockPessimistaRepository`,
`ConfirmacaoPessimistaService`, `LockPessimistaVsAtomicoComparativoIntegrationTest`, todos em
`src/test/java/br/com/desafio/tecnico/gestao/academico/concorrencia/`) permanece no repositório como
benchmark permanente, não como código morto a remover — nunca existiu no caminho de produção (D051), então
não há nada a excluir. Nenhum trade-off novo introduzido em produção.
**Riscos conhecidos / o que revisitar se o contexto mudar:** já registrado em D024 — "se a carga real de
matrícula concorrente crescer para uma escala de 'flash sale' (milhares de req/s pela mesma turma),
revisitar com reserva via Redis + fila". Esta comparação (N=10/20 alunos) não altera esse risco nem o
resolve — só confirma que, na escala testada, a estratégia atual continua sendo a escolha correta.

## D054 — Captura de evidências visuais via Playwright com browser real, sessão única e descartável

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, aceito sem alteração
**Spec relacionada:** specs/013-finalizacao.md
**Contexto:** O PRD pede evidência visual de que o sistema e a observabilidade funcionam de ponta a ponta
(tela de login, fluxo de matrícula, tela de administração, dashboard Grafana, trace no Jaeger, RabbitMQ
Management, Swagger UI). O ambiente de execução da IA não tem uma ferramenta de screenshot/browser nativa
disponível nesta sessão — foi preciso decidir como viabilizar a captura sem depender de mim (Pablo) rodar
tudo manualmente.
**Alternativas consideradas:**
- Um subagent instala um browser real via Playwright, só para esta sessão de captura (fora de
  `e2e/playwright/`, que deliberadamente não usa browser desde D049 — não entra no CI nem na suíte de
  concorrência) — automatiza a captura, mas introduz uma dependência de browser local mesmo que descartável.
- O Pablo captura manualmente, seguindo um roteiro exato (URL + ação + nome de arquivo esperado) preparado
  pela IA — evita qualquer instalação nova, mas consome tempo do Pablo e depende de execução manual fiel ao
  roteiro.
- Híbrido: subagent automatiza as telas simples (login, Swagger, RabbitMQ Management), Pablo captura as que
  dependem de timing/estado vivo mais difícil de scriptar (trace específico no Jaeger, dashboard Grafana com
  dado real).
**Decisão:** subagent com Playwright + browser real, sessão única e descartável.
**Justificativa:** apresentei a recomendação com o motivo (evita consumir tempo do Pablo numa tarefa
mecânica e repetitiva, que é exatamente o tipo de trabalho onde um agente é mais confiável que captura
manual apressada) e ele confirmou sem alteração.
**Trade-offs aceitos:** instalação de um browser Chromium local só para esta sessão (não persistida em
`e2e/playwright/`, não afeta CI/D049); evidências que já são texto por natureza (query em
`event_publication`, saída do teste de concorrência) viram bloco de código no documento em vez de imagem,
para não forçar uma captura de tela onde texto já é mais claro.
**Riscos conhecidos / o que revisitar se o contexto mudar:** nenhum identificado — a instalação é local,
temporária, e não se torna parte do toolchain permanente do projeto.

## D055 — `docs/OBSERVABILIDADE.md` dedicado, README mantém só resumo + link

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, aceito sem alteração
**Spec relacionada:** specs/013-finalizacao.md
**Contexto:** O Pablo pediu uma explicação de "papel + como funciona + como se conecta + URL/credenciais +
o que olhar primeiro" para cada componente real da stack de observabilidade (Prometheus, Grafana, Loki,
Promtail, Jaeger) — conteúdo substancial (~100+ linhas) que precisava de um lugar para viver.
**Alternativas consideradas:**
- Expandir a seção `## Observabilidade` já existente no README (hoje 34 linhas) — mantém tudo num arquivo
  só, mas faria o README (313 linhas) crescer bastante só nessa seção, competindo por atenção com o
  conteúdo operacional (como rodar, testes, autenticação).
- `docs/OBSERVABILIDADE.md` dedicado, README reduzido a um resumo curto + a tabela de URLs/credenciais já
  existente + link — mesmo padrão já usado no projeto (`docs/DECISIONS.md` = fonte granular,
  `docs/ARQUITETURA.md` = síntese, README = resumo operacional).
**Decisão:** `docs/OBSERVABILIDADE.md` dedicado.
**Justificativa:** confirmei a recomendação sem alteração — evita que o README fique desproporcional e seu
propósito operacional se perca, seguindo o mesmo padrão de separação já validado no projeto.
**Trade-offs aceitos:** mais um arquivo de documentação para manter sincronizado se a stack de
observabilidade mudar; mitigado pelo README linkar explicitamente, então a informação não fica "escondida".
**Riscos conhecidos / o que revisitar se o contexto mudar:** nenhum identificado.

## D056 — Diagrama de módulos via `Documenter` do Spring Modulith, comitado como `.puml`; fluxos narrativos em Mermaid

**Data:** 2026-07-13
**Origem:** 🤖 Default da IA, aceito sem alteração
**Spec relacionada:** specs/013-finalizacao.md
**Contexto:** O Pablo sugeriu avaliar gerar o diagrama de módulos com o `Documenter` do próprio Spring
Modulith (saída PlantUML/C4 a partir do teste de verificação de módulos já existente,
`ModularidadeTest.java`) como evidência automática de que a modularização declarada é a real. O ambiente
não tem PlantUML/Graphviz instalados para renderizar `.puml` em imagem.
**Alternativas consideradas:**
- Gerar o `.puml` via `Documenter` e tentar renderizar para PNG — exigiria instalar PlantUML + Graphviz só
  para esta tarefa, custo desproporcional ao ganho (o texto do `.puml` já é evidência válida por si só,
  inspecionável e renderizável em qualquer ferramenta/editor online de PlantUML).
- Gerar o `.puml` via `Documenter`, comitar como texto (sem renderizar), e usar diagramas Mermaid
  desenhados à mão para o conteúdo narrativo de `ARQUITETURA.md` (visão geral de módulos, fluxo do outbox)
  — Mermaid é renderizado nativamente pelo GitHub em Markdown, sem depender de ferramenta externa.
- Não gerar nenhum diagrama automático, só Mermaid manual — mais simples, mas perde a evidência automática
  de que a modularização declarada bate com o código real (o `Documenter` deriva do mesmo
  `ApplicationModules.of(...)` que `ModularidadeTest.verify()` já usa para reforçar os limites em tempo de
  teste).
**Decisão:** gerar o `.puml` via `Documenter` (evidência bruta complementar, comitada como texto em
`docs/architecture/modules.puml`), diagramas principais de `ARQUITETURA.md` em Mermaid manual.
**Justificativa:** confirmei a recomendação sem alteração — combina a evidência automática pedida pelo
Pablo com diagramas legíveis sem exigir ferramenta externa para quem só quer ler o documento no GitHub.
**Trade-offs aceitos:** o `.puml` não é renderizado neste repositório (só texto) — quem quiser a versão
visual precisa colar em uma ferramenta de PlantUML; aceitável porque não é o diagrama principal do
documento, só evidência complementar.
**Riscos conhecidos / o que revisitar se o contexto mudar:** se o projeto crescer o suficiente para
justificar CI gerando artefatos de documentação, revisitar renderização automática do `.puml`.

