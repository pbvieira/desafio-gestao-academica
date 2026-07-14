# 013 — Finalização (Fase 7 renomeada)

**Status:** `aprovada`
**Seção(ões) do PRD relacionadas:** §04 (documentação arquitetural), §05 (README esperado), §06 (critérios
críticos de avaliação), §07 (diferenciais), §08 (como a entrega será analisada), §09 (entrevista técnica)
**Módulo(s) Modulith afetado(s):** nenhum — só documentação (`docs/`, `README.md`, `specs/010`) e evidências
(`docs/images/`)

## 1. Objetivo

Fechar as lacunas reais de documentação/evidência que faltam para o desafio ser avaliado com segurança:
documento arquitetural curto (PRD §04, hoje inexistente como arquivo próprio), explicação legível da
observabilidade, evidência visual de ponta a ponta, auditoria literal contra o PRD §06/§08, e material de
entrevista consolidado. Não adiciona funcionalidade nova nem revisita regra de negócio já fechada.

## 2. Escopo

**Dentro do escopo:**
- `docs/ARQUITETURA.md`: visão geral, 3-5 decisões mais defensáveis com números reais, seção dedicada
  explicando o outbox implementado via Spring Modulith (Event Publication Registry), riscos conhecidos
  aceitos, caminho de evolução para múltiplos serviços, resumo de tratamento de falhas de mensageria.
- `docs/architecture/modules.puml`: saída do `Documenter` do Spring Modulith, evidência automática da
  modularização declarada (D056).
- `docs/OBSERVABILIDADE.md`: papel/funcionamento/conexão/URL/credenciais/"o que olhar primeiro" para
  Prometheus, Grafana, Loki, Promtail, Jaeger — incluindo o painel `matricula.vaga.conflito` (D052) ainda
  não documentado em lugar nenhum.
- Correção de inconsistências já encontradas: `specs/010` com `Status: aprovada` desatualizado; banner de
  status no topo do README desatualizado; 2 arquivos de plano não rastreados em
  `docs/superpowers/plans/`.
- Checklist literal do PRD §04/§05/§06/§08 com evidência apontada (arquivo/seção/teste), não assumida —
  vira a seção 6 desta spec.
- Gate final: `./mvnw clean verify` + suíte `e2e/` completa (bash + Playwright de concorrência) + frontend
  (`ng build`/`ng test`), numa mesma rodada, ambiente inteiro no ar.
- Sessão de captura de evidências visuais (`docs/images/`), embutidas no README/docs com legenda.
- Revisão da seção "Uso de IA" do README frente ao pedido literal do PRD §01/§05 (quais trechos mais
  críticos).
- "Pitch" de entrevista consolidado: índice pergunta provável (PRD §09) → onde está a resposta pronta.

**Fora do escopo (não-objetivos):**
- Qualquer funcionalidade de negócio nova.
- Qualquer mudança de regra de negócio.
- Retrabalho de decisões técnicas já fechadas — exceto se a checklist da seção 5/6 revelar uma lacuna real
  (não estética), caso em que a lacuna é reportada explicitamente antes de qualquer correção.
- Renderização de diagramas PlantUML para imagem (sem ferramenta disponível no ambiente, D056) — o `.puml`
  fica como texto/evidência complementar.

## 3. Regras de negócio envolvidas

Nenhuma — tarefa de documentação e evidência, sem mudança de comportamento do sistema.

## 4. Abordagem técnica

Ver plano de execução completo em `docs/superpowers/plans/2026-07-13-finalizacao.md` (a ser criado),
executado via skill `subagent-driven-development`, na ordem prescrita (não reordenável): (1)
`docs/ARQUITETURA.md`, (2) `docs/OBSERVABILIDADE.md` + resumo no README, (3) consistência
README/ROADMAP/DECISIONS/specs, (4) checklist do PRD com evidência, (5) gate e2e completo + captura, (6)
evidências visuais no README, (7) revisão do "Uso de IA", (8) pitch de entrevista + fechamento.

Cada novo documento narra e linka pontualmente para `docs/DECISIONS.md` (fonte granular de 53+ decisões) em
vez de reescrever o conteúdo — mesmo padrão já usado no projeto entre README (operacional) e DECISIONS.md
(granular).

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link para DECISIONS.md) |
|---|---|---|---|
| 1 | Como capturar evidências visuais sem ferramenta de screenshot nativa | subagent+Playwright browser real vs. captura manual pelo Pablo vs. híbrido | [D054](../docs/DECISIONS.md#d054) — subagent + Playwright, sessão única e descartável |
| 2 | Onde vive a explicação expandida de observabilidade | `docs/OBSERVABILIDADE.md` dedicado vs. expandir a seção do README | [D055](../docs/DECISIONS.md#d055) — `docs/OBSERVABILIDADE.md` dedicado |
| 3 | Como gerar o diagrama de módulos | `Documenter` do Modulith renderizado vs. `.puml` como texto + Mermaid manual vs. só Mermaid manual | [D056](../docs/DECISIONS.md#d056) — `.puml` via Documenter (texto) + Mermaid manual nos fluxos narrativos |
| 4 | Achado durante a Task 6: 6 testes de integração caem para RabbitMQ `guest/guest` ao desligar `spring.docker.compose.enabled` para isolar o Postgres | `RabbitMQContainer` dedicado por classe (espelha padrão existente) vs. apontar para o RabbitMQ compartilhado do compose | [D059](../docs/DECISIONS.md#d059) — `RabbitMQContainer` dedicado, mesmo padrão já usado em `MensageriaConfiabilidadeIntegrationTest`/`OutboxReenvioIntegrationTest` |

## 6. Critérios de aceite

Checklist literal do PRD, item por item — cada linha marcada `[x]` só quando houver evidência real
apontada (arquivo/seção/teste), preenchida progressivamente pelas Tasks 2-9 deste plano, auditada
formalmente na Task 5.

### PRD §04 — Requisitos Específicos para Dev Sênior

- [x] Solução modular ou baseada em mais de um serviço — evidência: `docs/ARQUITETURA.md` §"Visão geral"
  (6 pacotes de nível superior reconhecidos como módulos por `ApplicationModules.of(...)`) +
  `src/test/java/br/com/desafio/tecnico/gestao/ModularidadeTest.java` (`modulosRespeitamOsLimitesDeEncapsulamento()`
  chama `ApplicationModules.of(GestaoApplication.class).verify()`) + `docs/architecture/modules.puml`
  (aberto e confirmado: 5 componentes C4 — Academico, Administracao, Errorhandling, Notificacao,
  Security — gerados pelo `Documenter` real).
- [x] Separação clara entre contexto acadêmico e contexto de notificações/auditoria/relatórios —
  evidência: `src/main/java/br/com/desafio/tecnico/gestao/{academico,notificacao}` (pacotes confirmados
  via `ls`), `docs/ARQUITETURA.md` §"Visão geral" (explicita que os dois módulos só trocam eventos de
  domínio, nunca chamada Java síncrona).
- [x] Comunicação assíncrona usando RabbitMQ, Kafka ou equivalente — evidência:
  `src/main/java/br/com/desafio/tecnico/gestao/config/MensageriaConfig.java` (topologia real de
  exchange/filas/DLQ), README §"Mensageria assíncrona (RabbitMQ)", `docs/DECISIONS.md` D002.
- [x] Publicação de eventos de domínio (`MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada`) —
  evidência: os 3 records confirmados em `src/main/java/br/com/desafio/tecnico/gestao/academico/` com
  anotação `@Externalized`, chamados por `MatriculaService.java` (`criar()`/`confirmar()`/`cancelar()`).
- [x] Consumidor de eventos em outro módulo ou serviço — evidência:
  `src/main/java/br/com/desafio/tecnico/gestao/notificacao/MatriculaNotificacaoListener.java` (confirmado:
  `package ...notificacao;` + `@RabbitListener(queues = MensageriaConfig.FILA_MATRICULA)`).
- [x] Garantia de consistência na regra de vagas — evidência: `docs/DECISIONS.md` D024 (`UPDATE`
  condicional atômico `WHERE vagas_ocupadas < limite_vagas AND version = ?`) e D053 (decisão final),
  `TurmaRepository.consumirVaga`, `src/test/java/.../academico/MatriculaConcorrenciaIntegrationTest.java`
  (arquivo confirmado presente).
- [x] Preocupação explícita com concorrência na matrícula — evidência: `specs/012-concorrencia-e-testes.md`
  (Status: `concluída`, confirmado), D053, `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts`
  (20 confirmações simultâneas via `Promise.all`, 20 execuções totais, 1×200/19×409 em todas — números
  citados em `docs/ARQUITETURA.md` e no README).
- [x] Testes unitários e de integração nas regras críticas — evidência: gate JaCoCo ≥80% configurado em
  `pom.xml` (`<rule><element>BUNDLE</element>...<minimum>0.80</minimum>` confirmado, linha ~259),
  `src/test/java/.../academico/service/MatriculaServiceTest.java` e
  `MatriculaConcorrenciaIntegrationTest.java` (ambos confirmados presentes).
- [x] Docker Compose subindo aplicação, banco de dados e mensageria — **fechado na Task 5b (D057)**:
  `Dockerfile` (multi-stage, build com Maven wrapper + runtime JRE) + serviço `app` em `compose.yaml`
  (`network_mode: host`, atrás do profile `app`) — `docker compose --profile app up -d --build` sobe
  aplicação, Postgres e RabbitMQ num único comando. Validado de ponta a ponta nesta task: `GET
  /actuator/health` → `{"status":"UP"}` com a app containerizada; token real obtido do Keycloak do compose
  e usado contra `GET /api/turmas` da app containerizada → 401 sem token / 200 com token (confirma que o
  `issuer-uri` bateu com o `iss` do JWT real, mesmo em container); `e2e/smoke-test.sh` e
  `e2e/matricula-flow.sh` (fluxo completo criar→confirmar→duplicidade→cancelar→vaga liberada, incluindo
  consumo real do evento pelo RabbitMQ) rodados com sucesso contra a app containerizada. `./mvnw
  spring-boot:run` (D003) continua documentado e funcional como opção alternativa mais rápida para
  desenvolvimento ativo — ver README §"Como rodar localmente".
- [x] Observabilidade mínima: logs estruturados, correlation/trace ID, health checks, métricas básicas —
  evidência: `docs/OBSERVABILIDADE.md` (aberto e lido por completo — cobre Prometheus/Grafana/Loki/
  Promtail/Jaeger, papel/funcionamento/conexão/URL/credenciais de cada um), README §"Observabilidade"
  (`GET /actuator/health` público confirmado), `docs/DECISIONS.md` D034 (trace ID).
- [x] **Documentação arquitetural curta (decisões, trade-offs, riscos conhecidos, evolução, tratamento de
  falhas de mensageria)** — evidência: `docs/ARQUITETURA.md` (aberto e lido por completo — contém as 6
  seções pedidas: visão geral, decisões defensáveis com números reais, outbox, riscos conhecidos aceitos,
  caminho de evolução, tratamento de falhas de mensageria).
- [x] README completo — evidência: `README.md` (311 linhas, 11 seções de nível 2 confirmadas via
  `grep '^##'` — ver mapeamento item a item do §05 abaixo).

### PRD §05 — README Esperado (11 itens)

- [x] Como rodar o projeto localmente — evidência: README §"Como rodar localmente" (linhas 13-35, passos
  numerados 1-3 + pré-requisitos) e §"Frontend (Angular)" (linhas 37-76, `npm install`/`npm start`).
- [x] Como subir a solução com Docker Compose — evidência: README §"Como rodar localmente" (`docker compose
  up -d` e `docker compose --profile observability up -d`, linhas 21-28).
- [x] Como executar os testes automatizados — evidência: README §"Como rodar os testes" (linhas 78-112:
  `./mvnw clean verify`, 3 scripts `e2e/*.sh`, Playwright `--repeat-each=10`).
- [x] Quais tecnologias foram usadas — evidência: README §"Tecnologias" (linhas 114-118).
- [x] Quais foram as principais decisões técnicas — evidência: README §"Documentação" (linha 298, aponta
  para `docs/DECISIONS.md`) + decisões citadas inline ao longo de todo o README (D002, D003, D008, D009,
  D024, D032-D035, D036, D042, D045, D046, D053 todos referenciados no corpo do texto, não só na seção
  final).
- [x] Como a regra de vagas foi protegida — evidência: README §"Proteção de vaga e prova de concorrência"
  (linhas 162-171, `UPDATE` condicional atômico com o SQL exato citado).
- [x] Como a solução trata concorrência na matrícula — evidência: mesma seção, §"Prova real (não teórica)"
  (linhas 173-189, números reais: 20 execuções, 1×200/19×409).
- [x] Como os eventos de domínio são publicados e consumidos — evidência: README §"Mensageria assíncrona
  (RabbitMQ)" (linhas 120-134).
- [x] Como a solução trata falhas na mensageria — evidência: mesma seção, §"Confiabilidade sob falha do
  broker (outbox)" e o fluxo de retry/DLQ (linhas 130-134, 152-156).
- [x] Quais logs, health checks, métricas ou mecanismos de rastreabilidade foram implementados —
  evidência: README §"Observabilidade" (linhas 191-217: tabela de URLs/credenciais, dashboard provisionado,
  endpoints do Actuator expostos) + §"Mensageria..." §"Rastreabilidade (trace ID)" (linhas 144-150).
- [x] Quais ferramentas de IA foram utilizadas, em quais partes, o que foi revisado manualmente e quais
  trechos são mais críticos — evidência: README §"Uso de IA" (linhas 385-437, fechado na Task 8 deste plano,
  commit `d33171c`) — enumera a contagem por origem (17 decisões ativas do Pablo / 10 sugestões da IA
  revisadas / 31 defaults aceitos / 1 origem mista, `docs/DECISIONS.md`) e lista explicitamente os 5 trechos
  de maior peso técnico com âncora real (D024/D053, D034, D031, D059, bug do dropdown de papel).

(Evidência de cada item: seção correspondente do `README.md`, mapeada linha a linha acima, confirmada por
leitura direta do arquivo nesta task.)

### PRD §06 — Critérios Críticos de Avaliação (eliminatórios — 12 itens, todos precisam de evidência de
que o problema NÃO ocorre)

- [x] Execução: aplicação roda de forma reproduzível, com instrução clara — evidência: README
  §"Como rodar localmente", instruções claras e sequenciais (`.env` → `docker compose up -d`), com duas
  opções documentadas para a aplicação em si: `./mvnw spring-boot:run` (D003) ou, a partir da Task 5b
  (D057), `docker compose --profile app up -d --build` — um único comando cobrindo aplicação, banco de
  dados e mensageria (ver item §04 correspondente acima, agora `[x]`).
- [x] Stack: backend em Spring Boot — evidência: `pom.xml` (`spring-boot-starter-parent` 3.5.16,
  confirmado no cabeçalho já lido em sessões anteriores do projeto e reconfirmado por
  `grep spring-boot-starter-web pom.xml`), pacote base `br.com.desafio.tecnico.gestao`.
- [x] Persistência: há persistência de dados real (Postgres) — evidência: `compose.yaml` serviço
  `postgres` (`postgres:latest`, confirmado), `src/main/resources/db/migration/V1..V9` (9 migrations
  Flyway confirmadas via `ls`, cobrindo Aluno/Curso/Disciplina/Turma/Matrícula + infraestrutura de eventos).
- [x] Regras de negócio: regra de matrícula implementada, regra de vagas não quebra sob concorrência —
  evidência: `TurmaRepository.consumirVaga` (UPDATE condicional), `MatriculaConcorrenciaIntegrationTest`,
  `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts` (20/20 execuções sem quebra, números
  citados acima).
- [x] Camadas: separação clara de responsabilidades, não concentrado em controllers — evidência: pacote
  `academico` com subpacotes `web` (controller), `service`, `domain`, `repository`, `dto` (confirmado via
  `find`); `MatriculaController.java` tem 61 linhas vs. `MatriculaService.java` com 197 linhas — lógica de
  negócio está no service, não no controller (checado por contagem de linhas, não assumido).
- [x] Testes: há testes para regras críticas de matrícula — evidência: `MatriculaServiceTest.java`,
  `MatriculaConcorrenciaIntegrationTest.java` (ambos confirmados presentes em `src/test/java/.../academico/`).
- [x] Erros: tratamento padronizado de erros (ProblemDetail) — evidência:
  `src/main/java/br/com/desafio/tecnico/gestao/errorhandling/GlobalExceptionHandler.java` +
  `ProblemDetailFactory.java` (confirmados presentes, uso de `ProblemDetail` grepado em 4 arquivos),
  `docs/DECISIONS.md` D016 (propriedade `errorCode` customizada no `ProblemDetail`).
- [x] Mensageria: há mensageria real com consumidor em outro módulo/serviço — evidência: `compose.yaml`
  serviço `rabbitmq` real (não em memória), `MatriculaNotificacaoListener.java` no pacote `notificacao`
  (módulo diferente de `academico`, que publica) com `@RabbitListener` real.
- [x] Arquitetura: separação em módulos não é artificial, tem ganho arquitetural real e defensável —
  evidência: `ModularidadeTest.modulosRespeitamOsLimitesDeEncapsulamento()` **falha o build** se a
  fronteira for violada (não é só uma convenção de pasta) + `docs/ARQUITETURA.md` explicita que
  `academico`/`notificacao` só se comunicam por evento de domínio, nunca chamada Java síncrona — o
  desacoplamento é verificável em teste, não apenas declarado em prosa.
- [x] Consistência: há preocupação real com consistência/concorrência (prova, não afirmação) — evidência:
  prova via Playwright com HTTP/Keycloak reais (não MockMvc simulado), 20 execuções, números exatos
  reportados (1×200/19×409 em todas), comparação JVM isolada atômica-vs-pessimista documentada em
  `docs/ARQUITETURA.md` §"D024/D053" com números de tempo reais (32ms vs. 33ms) — é prova, não afirmação.
- [x] Documentação: há documentação arquitetural e README suficientes para executar e validar — evidência:
  `docs/ARQUITETURA.md` + `docs/OBSERVABILIDADE.md` + `README.md`, todos abertos e lidos por completo
  nesta task.
- [x] Entrevista: o candidato consegue explicar as decisões, o código e o uso de IA (pitch consolidado) —
  evidência: `docs/ARQUITETURA.md` §"Pitch de entrevista: índice pergunta → resposta pronta" (Task 9 deste
  plano, esta task) — cobre as 10 perguntas literais do PRD §09, cada uma com ponteiro real e conferido
  (arquivo/método/linha, seção de spec ou âncora de `docs/DECISIONS.md`), incluindo um trecho de código
  recitável de cor (`TurmaRepository.consumirVaga`) para a pergunta "explique sem consultar documentação".

### PRD §07 — Diferenciais (informativo, não eliminatório — registrar presença/ausência)

- [x] Outbox Pattern — presente. Evidência: `docs/ARQUITETURA.md` §"Outbox: como funciona neste projeto"
  (Event Publication Registry do Spring Modulith, tabela `event_publication` via
  `V8__infraestrutura_criar_tabela_event_publication.sql`, confirmada aberta e com o comentário SQL
  citando explicitamente "spring-modulith-events-jpa").
- [x] Idempotência no consumo de mensagens — presente. Evidência: tabela `evento_processado`
  (`V9__notificacao_criar_tabela_evento_processado.sql`, confirmada aberta — comentário SQL cita dedupe
  por `eventId`), consumida por `MatriculaNotificacaoListener.java`.
- [x] Retry e dead letter queue — presente. Evidência: `MensageriaConfig.java`, README §"Mensageria
  assíncrona (RabbitMQ)" (retry com TTL 10s, 3 tentativas, DLQ `notificacao.matricula.dlq`),
  `docs/DECISIONS.md` D033.
- [x] Tracing distribuído — presente. Evidência: `docs/OBSERVABILIDADE.md` §"Jaeger" (OTLP HTTP `:4318`,
  `management.tracing.sampling.probability=1.0`), serviço `jaeger` confirmado em `compose.yaml`.
- [x] Autenticação e autorização — presente. Evidência: serviço `keycloak` em `compose.yaml`, README
  §"Autenticação (Keycloak)" (papéis `ALUNO`/`SECRETARIA`/`ADMIN`, RBAC/ABAC), specs 003/010.
- [x] CI/CD — presente. Evidência: `.github/workflows/ci.yml` (confirmado aberto — job `frontend` roda
  `ng test`/`ng build`, job `build` roda `./mvnw clean verify` + sobe `docker compose --profile
  observability` + roda a aplicação).
- [x] ADRs curtos — presente, em formato equivalente. Evidência: `docs/DECISIONS.md` (59 entradas
  confirmadas via `grep -c '^## D[0-9]'`), cada uma com contexto/alternativas/decisão/trade-offs/riscos —
  não usa o template ADR nomeado literalmente, mas cobre a mesma função.
- [x] Estratégia clara para refatoração de legado (caminho de evolução) — presente. Evidência:
  `docs/ARQUITETURA.md` §"Caminho de evolução" (passo a passo concreto de como extrair `notificacao` para
  um serviço separado, respondendo diretamente à pergunta do PRD §09).
- [ ] Paginação e filtros — **ausente, confirmado por busca no código**. `grep -rn "Pageable\|Page<"
  src/main/java` não retornou nenhuma ocorrência; os endpoints de listagem (`GET /api/alunos`,
  `/api/cursos`, `/api/disciplinas`, `/api/turmas`, `/api/administracao/usuarios`) não usam
  `@RequestParam` para filtro nem paginação — retornam a lista completa. É um diferencial não obrigatório
  (PRD §07), sua ausência não é eliminatória, mas é uma lacuna real, não uma suposição.
- [x] Boa organização do frontend — presente. Evidência: `frontend/src/app/{core,features,shared}`
  (confirmado via `find`), com `features/` subdividido por domínio (`turma`, `matricula`, `aluno`, `curso`,
  `disciplina`, `administracao`) — organização por feature, não por tipo de arquivo.
- [x] Boa explicação sobre uso de IA — presente, evidência igual ao item §05 correspondente acima: README
  §"Uso de IA" reforçado na Task 8 deste plano (commit `d33171c`).

### PRD §08 — Como a Entrega Será Analisada (9 dimensões — qualitativas, evidência mais forte disponível
para cada uma, não um artefato único)

- [x] Funcionalidade entregue — evidência: CRUD de Aluno/Curso/Disciplina/Turma + fluxo de matrícula
  completo (`academico/web/*Controller.java`, 5 controllers confirmados), `e2e/matricula-flow.sh` (fluxo
  ponta a ponta citado no README).
- [x] Organização do backend — evidência: pacotes `academico/{web,service,domain,repository,dto}`
  confirmados, `docs/ARQUITETURA.md` §"Visão geral".
- [x] Regras de negócio — evidência: `TurmaRepository.consumirVaga`, `MatriculaService.java` (197 linhas,
  três métodos principais `criar()`/`confirmar()`/`cancelar()` citados em `docs/ARQUITETURA.md`).
- [x] Testes — evidência: gate JaCoCo ≥80% no `pom.xml`, `MatriculaServiceTest`,
  `MatriculaConcorrenciaIntegrationTest`, `OutboxReenvioIntegrationTest` (todos confirmados presentes).
- [x] Frontend — evidência: `frontend/src/app/{core,features,shared}`, README §"Frontend (Angular)".
- [x] Documentação — evidência: `README.md` (311 linhas), `docs/ARQUITETURA.md`, `docs/OBSERVABILIDADE.md`,
  `docs/DECISIONS.md` (59 decisões), Swagger UI (`OpenApiConfig.java` confirmado presente).
- [x] Arquitetura/desacoplamento — evidência: `ModularidadeTest.verify()` (limite reforçado em teste, não
  só declarado), `docs/ARQUITETURA.md` §"Caminho de evolução".
- [x] Mensageria/eventos — evidência: `MensageriaConfig.java`, outbox via Event Publication Registry,
  idempotência (`evento_processado`), retry+DLQ, tudo documentado em `docs/ARQUITETURA.md` §"Outbox" e
  §"Tratamento de falhas de mensageria".
- [x] Observabilidade/operação — evidência: `docs/OBSERVABILIDADE.md` completo, `/actuator/health`
  público, `/actuator/prometheus` protegido por Basic Auth, dashboard Grafana provisionado
  (`docker/grafana/provisioning/dashboards/jvm-http-dashboard.json`, arquivo confirmado presente e
  confirmado conter a métrica `matricula_vaga_conflito_total` via `grep` direto no JSON, não apenas citado
  de segunda mão).

### Gate final (não é item do PRD, é verificação operacional desta spec)

- [x] `./mvnw clean verify` verde — rodado na Task 6 deste plano (147 testes, 0 falhas/erros, gate JaCoCo
  mantido, confirmado após o fix D059 no commit `0fc6730` e novamente ao final da Task 6, commit `9bd473d`).
- [x] Suíte `e2e/` completa verde (3 scripts bash + Playwright `--repeat-each=10`) — Task 6, 2ª/3ª tentativa:
  os 3 scripts bash e a suíte Playwright de concorrência rodaram 20/20 (`--repeat-each=10`, duas vezes) sem
  falha, contra a stack completa de pé.
- [x] `ng build` + `ng test --watch=false` verdes — confirmado na Task 6 (correção do bug de dropdown de
  papel, commit `9a31930`): 26/26 testes (`npx ng test --watch=false --browsers=ChromeHeadless`, suíte
  completa, não só o teste novo) + `ng build` verdes, rodados diretamente antes do commit.
- [x] Evidências visuais capturadas e embutidas com legenda (nenhuma imagem "solta") — 7 screenshots
  capturados na Task 6 (commits `9bd473d`/`c1e7ec6`) e embutidos com legenda no README/ARQUITETURA/
  OBSERVABILIDADE na Task 7 (commit `0fd43bf`), review clean.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário/Integração | Nenhum novo — esta fase não altera código de produção | N/A |
| E2E | Gate final roda toda a suíte já existente (`e2e/*.sh` + `e2e/playwright/`) numa mesma rodada, sem nada quebrado | `e2e/` |

**Justificativa de cobertura:** esta fase é documentação e evidência, não código — não há regra de negócio
nova para testar. O "teste" real desta fase é o gate e2e completo (Task 6) confirmando que nada regrediu, e
a auditoria de checklist (Task 5) confirmando que cada afirmação documentada tem evidência real por trás.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhuma mudança de código — só documentação melhor do que já existe (inclusive
  cobrindo o painel `matricula.vaga.conflito` hoje não documentado).
- **Segurança:** nenhum endpoint novo/alterado. Atenção ao capturar evidências visuais: não expor
  credenciais reais em prints (usar só as credenciais de teste dev-only já documentadas, nunca segredo de
  produção — não há segredo de produção neste projeto, mas o cuidado vale como prática).

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos — checklist do PRD §04-§08 completo com evidência real. Os 3
  itens que dependiam de sequenciamento (README §"Uso de IA" genérico → Task 8; "Entrevista"/pitch → Task 9)
  foram fechados nas respectivas tasks e marcados `[x]` acima com evidência real. Único item genuinamente
  ausente e corretamente marcado `[ ]`: "Paginação e filtros" (PRD §07, diferencial informativo, não
  eliminatório) — confirmado ausente por busca literal no código, não uma lacuna escondida.
- [x] Gate final (seção 6) passando — os 4 itens rodaram e passaram na Task 6 deste plano (ver evidência de
  cada um marcada acima: `./mvnw clean verify` 147 testes verde, e2e bash+Playwright 20/20, `ng
  build`/`ng test` 26/26 verdes, 7 screenshots capturados e embutidos).
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D054-D059 registradas — D054-D056 na
  Task 1, D057 na Task 5/5b, D059 no achado da Task 6; nenhuma decisão desta fase ficou fora do log).
- [ ] `code-reviewer` executado — **pendente**: roda sobre o diff completo da fase (Task 1 até esta task),
  como revisão final de branch, depois desta Task 9 — não antecipado aqui (mesmo erro já cometido e
  corrigido na Fase 6, spec 012, commit `aa60b2e`; ver "Depois da Task 9" no brief de execução desta task).
- [ ] `security-auditor` executado — **pendente**, mesmo motivo do item acima: roda junto com o
  `code-reviewer` na revisão final de branch, ainda não disparada nesta sessão.
- [x] Esta spec atualizada para refletir o que foi de fato implementado — seção 6 (3 itens sequenciados +
  gate final) e esta seção 9 atualizadas nesta task com evidência real, não placeholder.
- [x] `./mvnw clean verify` passando — confirmado verde na Task 6 (ver Gate final acima); nenhuma mudança de
  código de produção desde então nesta fase (Tasks 7-9 são só documentação).
