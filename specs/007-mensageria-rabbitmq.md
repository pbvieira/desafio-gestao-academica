# 007 — Mensageria assíncrona real: externalização para RabbitMQ

**Status:** `aprovada`
**Seção(ões) do PRD relacionadas:** §04 (mensageria assíncrona real, consumidor em módulo separado),
§07 (diferenciais: idempotência, observabilidade de mensageria)
**Módulo(s) Modulith afetado(s):** `academico` (eventos ganham `@Externalized` + `eventId`); `notificacao`
(consumidor real via `@RabbitListener` + tabela de idempotência); `config` (topologia RabbitMQ)

## 1. Objetivo

Externalizar para o RabbitMQ os eventos de domínio que a Fase 3 já publica internamente via Spring
Modulith (`MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada`), com consumidor real em módulo
separado, outbox (Event Publication Registry, já existe desde a Fase 3), retry + DLQ nativos do RabbitMQ, e
idempotência no consumo — a mensageria assíncrona real exigida pelo PRD para nível sênior.

## 2. Escopo

**Dentro do escopo:**
- `spring-modulith-events-amqp` no `pom.xml` — externaliza os eventos já publicados via
  `ApplicationEventPublisher` (Fase 3) sem reescrever a lógica de publicação em `MatriculaService`.
- Topologia RabbitMQ (D032): exchange topic `gestao.eventos`; fila `notificacao.matricula` (bound com
  `matricula.#`); fila de retry `notificacao.matricula.retry` (TTL, D033); fila `notificacao.matricula.dlq`
  (destino final).
- `eventId` (UUID) adicionado aos 3 records de evento (D035) — necessário para idempotência.
- Confirmação/ativação de `republish-outstanding-events-on-restart=true` sobre o Event Publication Registry
  já existente (`event_publication`, `V8` da Fase 3) — o outbox já existe, esta fase só liga e prova a
  garantia de reenvio.
- Retry nativo do RabbitMQ (D033): 3 tentativas, TTL fixo de 10s, DLQ final.
- Consumidor real: `MatriculaNotificacaoListener` troca `@ApplicationModuleListener` por `@RabbitListener`
  consumindo de `notificacao.matricula`.
- Idempotência: tabela `evento_processado` (módulo `notificacao`) com dedupe por `eventId`.
- Trace ID propagado até o consumidor (D034) — **implementado via captura síncrona na thread da requisição
  HTTP + campo no payload do evento**, não via Observation nativa: verificação empírica confirmou que nem a
  config nativa (`observation-enabled`) nem um `MessagePostProcessor` no `RabbitTemplate` funcionam, porque
  a externalização do Modulith roda numa thread assíncrona sem o contexto de span da requisição original
  (ver seção 4.5 e D034 para o histórico completo).
- Métrica Micrometer de mensagens enviadas à DLQ (`mensageria.dlq.eventos`), visível no Prometheus/Grafana.
- README atualizado: como inspecionar a DLQ via RabbitMQ Management UI.
- Teste de integração de outbox (falha simulada entre persistir e publicar, reenvio no restart), teste de
  retry→DLQ, teste de idempotência (redelivery não duplica processamento).
- Validação manual ponta a ponta: publicar evento real, forçar falha no consumidor, confirmar retry→DLQ via
  Management UI, confirmar que reenviar manualmente a mesma mensagem não duplica o processamento.
- E2E novo ou extensão do `e2e/matricula-flow.sh`: criar matrícula → evento publicado → consumidor processa
  → registro de idempotência criado.

**Fora do escopo (não-objetivos):**
- Notificação real (e-mail/push) no consumidor — continua sendo só o log estruturado "notificação seria
  enviada aqui" da Fase 3.
- Múltiplos consumidores/filas para finalidades diferentes — um consumidor de referência
  (`notificacao.matricula`) é suficiente para provar o mecanismo (D035).
- Qualquer lógica de domínio nova de Matrícula — fechou na Fase 3, não é reaberta aqui.
- Backoff incremental entre tentativas de retry, comparação de estratégias — Fase 7, mesmo espírito de D024
  (D033).
- Autenticação/TLS na rede interna do compose entre app e RabbitMQ — aceitável em dev (rede do compose não
  é exposta), registrado como risco conhecido a mitigar em produção (seção 8).

## 3. Regras de negócio envolvidas

Esta fase não altera nenhuma regra de negócio de Matrícula (fechada na Fase 3) — as "regras" aqui são de
confiabilidade de mensageria:

- [x] Evento de domínio publicado é persistido antes de ser externalizado (outbox, já existente) — se a
  aplicação cair entre persistir a Matrícula e publicar/externalizar o evento, o evento não se perde e é
  reenviado no próximo start. (`OutboxReenvioIntegrationTest`, dois contextos Spring reais.)
- [x] Mensagem que falha no processamento do consumidor é reprocessada automaticamente (retry com TTL) até
  3 tentativas; após isso, vai para a DLQ, nunca é perdida silenciosamente. (`MensageriaConfiabilidadeIntegrationTest`
  + validação manual, seção 10.)
- [x] Redelivery da mesma mensagem (ex: após falha de ack) não duplica o efeito do processamento
  (idempotência via `evento_processado`). (`MensageriaConfiabilidadeIntegrationTest` + validação manual,
  seção 10.)
- [x] Trace ID da requisição HTTP original aparece no log do consumidor (e, se cair na DLQ, é rastreável).
  (`MatriculaNotificacaoListenerTest#processarCriada_comTraceId_...`, `MensageriaConfiabilidadeIntegrationTest`,
  validação manual via `e2e/matricula-flow.sh`.)

## 4. Abordagem técnica

### 4.1 Dependências e topologia

`pom.xml`: adicionar `spring-modulith-events-amqp` (o BOM do Modulith já gerencia a versão, `1.4.12`).
`spring-boot-starter-amqp` já está no `pom.xml` desde o Initializr — não precisa adicionar.

`config.MensageriaConfig` (novo, `br.com.desafio.tecnico.gestao.config` — cross-cutting, mesmo padrão de
`OpenApiConfig`, D035): declara via `@Bean`:
- `TopicExchange gestaoEventosExchange()` → `gestao.eventos`, durable.
- `Queue matriculaQueue()` → `notificacao.matricula`, durable, com argumentos
  `x-dead-letter-exchange=""` (exchange padrão) e `x-dead-letter-routing-key=notificacao.matricula.retry`.
- `Binding matriculaBinding()` → liga `matriculaQueue` a `gestaoEventosExchange` com routing key
  `matricula.#`.
- `Queue matriculaRetryQueue()` → `notificacao.matricula.retry`, durable, com argumentos
  `x-message-ttl=10000` (10s, D033), `x-dead-letter-exchange=""`,
  `x-dead-letter-routing-key=notificacao.matricula` (bounce de volta à fila principal ao expirar o TTL) —
  **não é bound a nenhum exchange para consumo direto**, é só uma fila de espera.
- `Queue matriculaDlq()` → `notificacao.matricula.dlq`, durable — destino final, escrito explicitamente
  pelo consumidor (seção 4.4), não por dead-letter automático (a contagem de tentativas via `x-death`
  precisa de decisão da aplicação, não é um limite nativo configurável em fila clássica sem plugin extra).

### 4.2 Externalização dos eventos

`MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada` (`academico`, pacote raiz) ganham:
- Campo `UUID eventId` — gerado em `MatriculaService` na criação do record (`UUID.randomUUID()`), antes de
  `eventPublisher.publishEvent(...)`.
- Campo `String traceId` (D034) — capturado na mesma linha, via `Tracer` injetado em `MatriculaService`
  (`tracer.currentSpan()`), pelo mesmo motivo que `eventId`: precisa existir no momento da criação do
  record, na thread da requisição HTTP original (ver seção 4.5).
- Anotação `@Externalized(MensageriaConfig.EXCHANGE_EVENTOS + "::" + MensageriaConfig.ROUTING_KEY_MATRICULA_CRIADA)`
  (e equivalentes para os outros dois) — instrui `spring-modulith-events-amqp` a publicar no exchange
  `gestao.eventos` com essa routing key sempre que o evento for publicado internamente, sem qualquer mudança
  na lógica de negócio de `MatriculaService`. Referencia as constantes de `MensageriaConfig` em vez de
  literais soltos (achado de code review: evita duplicar a mesma string em publicador e consumidor sem
  fonte única — `@Externalized` aceita expressão de constante em tempo de compilação).

### 4.3 Outbox (Event Publication Registry) — já existe, esta fase liga e prova

A tabela `event_publication` (`V8`, Fase 3) já é o Event Publication Registry do Modulith — persiste cada
publicação de evento antes de entregá-la aos listeners/externalizadores, e marca `completion_date` quando
entregue com sucesso. Esta fase:
- Liga `spring.modulith.events.republish-outstanding-events-on-restart=true` em `application.properties`.
- Prova com teste de integração (seção 4.6): publica um evento, simula falha do
  consumidor/externalizador antes da confirmação, reinicia o contexto Spring, confirma que o evento
  incompleto (`completion_date IS NULL`) é reenviado.

Nenhuma migration nova é necessária para o outbox em si — só para a idempotência (seção 4.4).

### 4.4 Consumidor real + idempotência

`notificacao.MatriculaNotificacaoListener` troca os 3 métodos `@ApplicationModuleListener` por um único
`@RabbitListener(queues = "notificacao.matricula")` recebendo `org.springframework.amqp.core.Message`:
1. Extrai a routing key para saber qual tipo de evento desserializar. **Achado ao rodar o teste de
   integração (não previsto neste desenho original):** `message.getMessageProperties().getReceivedRoutingKey()`
   só reflete a routing key do ÚLTIMO hop de entrega — após um bounce pela fila de retry, ela vira o NOME DA
   FILA de destino (`notificacao.matricula`), não mais `matricula.criada`/etc. O consumidor final usa a
   entrada do header `x-death` com `queue=notificacao.matricula`/`reason=rejected`, cujo campo `routing-keys`
   preserva a routing key ORIGINAL, com fallback para `getReceivedRoutingKey()` só na primeira tentativa
   (quando `x-death` ainda não existe).
2. Extrai `eventId` do payload desserializado.
3. Verifica `evento_processado` (nova tabela, migration `V9__notificacao_criar_tabela_evento_processado.sql`:
   `id UUID PRIMARY KEY, processado_em TIMESTAMPTZ NOT NULL`) — se `eventId` já existe, loga e retorna sem
   reprocessar (idempotente); senão, processa (log estruturado "notificação seria enviada aqui", igual à
   Fase 3) e insere o `eventId` na mesma transação.
4. Em caso de exceção não tratada, deixa propagar — o container do listener (configurado com
   `default-requeue-rejected=false`, D035) faz `nack` sem requeue, RabbitMQ rotea para
   `notificacao.matricula.retry` via o dead-letter da fila principal.
5. Contagem de tentativas: o consumidor inspeciona o header `x-death` da mensagem recebida (lista de
   entradas que o RabbitMQ adiciona a cada dead-letter, com contador por fila/motivo). Se o número de
   dead-letters da fila `notificacao.matricula` já é ≥ 3 (D033) e o processamento falha de novo, o
   consumidor publica a mensagem explicitamente em `notificacao.matricula.dlq` (via `RabbitTemplate`) e
   loga em nível WARN + incrementa o contador Micrometer `mensageria.dlq.eventos`, em vez de deixar a
   exceção propagar para outro ciclo de retry.

### 4.5 Trace ID (D034) — mecanismo final, após verificação empírica

O plano original (`spring.rabbitmq.template/listener.observation-enabled=true`, reaproveitando a
infraestrutura nativa de Observation do Spring Boot/AMQP) **foi verificado empiricamente e não funciona**
para este caso: rodando a aplicação real (Postgres/Redis/RabbitMQ/Keycloak + stack de observabilidade via
compose), uma linha de log disparada por uma requisição HTTP mostra `traceId`/`spanId` normalmente, mas o
log do consumidor não, mesmo com as duas flags ligadas. Um fallback manual (`MessagePostProcessor` no
`RabbitTemplate`, lendo `tracer.currentSpan()` no momento do publish) **também foi tentado e também
falhou**, pelo mesmo motivo raiz: a externalização do Modulith roda de forma assíncrona numa thread própria
(ex: `task-1`), fora do escopo do `ThreadLocal` de span do Micrometer da requisição HTTP original — qualquer
captura feita no momento em que o Modulith efetivamente publica para o RabbitMQ chega tarde demais
(confirmado via log de diagnóstico: `tracer.currentSpan()` retornava `null` nessa thread, nos dois casos).

**Mecanismo final implementado:** o `traceId` é capturado na PRÓPRIA thread da requisição HTTP, dentro de
`MatriculaService` (método privado `traceIdAtual()`, com `Tracer` injetado via construtor), no exato ponto
de cada `eventPublisher.publishEvent(...)` — antes de qualquer despacho assíncrono do Modulith acontecer. O
valor viaja como campo `traceId` no próprio payload do evento (ao lado do `eventId`), não como header AMQP.
O consumidor lê esse campo genericamente via `ObjectMapper.readTree` (sem amarrar a um tipo de evento
específico, já que a routing key só é decidida depois — ver 4.4) e coloca em `MDC.put("traceId", ...)` antes
de despachar para o processamento, removendo num bloco `finally`. As duas flags de Observation nativa foram
mantidas em `application.properties` mesmo assim, pelo valor observacional que ainda oferecem (spans de
publish/consume visíveis no Jaeger), só não pelo motivo originalmente cogitado — comentário corrigido no
próprio arquivo de properties (achado de code review: o comentário original afirmava o oposto do que D034
documenta).

Ver `docs/DECISIONS.md#d034` para o histórico completo da investigação.

### 4.6 Testes de confiabilidade

- **Outbox**: publica um evento, intercepta/derruba a entrega antes da confirmação (ex: mockar o
  externalizador para lançar exceção na primeira tentativa), reinicia o contexto, confirma reenvio.
- **Retry → DLQ**: consumidor configurado para falhar deliberadamente (flag de teste), confirma que a
  mensagem passa pela fila de retry (TTL curto no teste, ex: 1-2s, via property override) e cai na DLQ
  após 3 tentativas.
- **Idempotência**: publica a mesma mensagem (mesmo `eventId`) duas vezes manualmente, confirma que só uma
  linha existe em `evento_processado` e que o efeito (log) não duplica.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Tipo de exchange | Topic vs. direct | [D032](../docs/DECISIONS.md#d032) — topic |
| 2 | Tentativas de retry / TTL | 3×10s fixo vs. 5×incremental | [D033](../docs/DECISIONS.md#d033) — 3×10s fixo |
| 3 | Propagação de trace ID | Observation nativa vs. `MessagePostProcessor` manual | [D034](../docs/DECISIONS.md#d034) — nativa cogitada inicialmente; revista após verificação empírica (nem nativa nem `MessagePostProcessor` funcionam) para captura na thread da requisição + campo no payload do evento |

Decisões menores agrupadas em [D035](../docs/DECISIONS.md#d035) (`eventId`, localização da topologia,
observabilidade da DLQ, consumidor único, `default-requeue-rejected=false`) — sinalizadas para revisão, não
bloqueantes.

## 6. Critérios de aceite

- [x] `spring-modulith-events-amqp` publica os 3 eventos no RabbitMQ real (visível no Management UI) sem
  nenhuma mudança na lógica de negócio de `MatriculaService` (só a injeção do `Tracer` para D034).
- [x] Fluxo real (criar → confirmar matrícula via API) resulta em mensagem consumida por
  `MatriculaNotificacaoListener` com log "notificação seria enviada aqui" (mesmo texto da Fase 3).
- [x] Consumidor forçado a falhar: mensagem aparece em `notificacao.matricula.retry` (TTL), depois volta
  para `notificacao.matricula`, e após 3 tentativas aparece em `notificacao.matricula.dlq` — visível no
  Management UI (validado manualmente, seção 10, e via `MensageriaConfiabilidadeIntegrationTest`).
- [x] Reenviar manualmente (via Management UI) a mesma mensagem que já foi processada com sucesso não
  duplica o efeito (idempotência) — só uma linha em `evento_processado` por `eventId` (validado manualmente,
  seção 10).
- [x] Reinício da aplicação após uma falha simulada entre persistir e externalizar reenvia o evento
  pendente (outbox).
- [x] `traceId` da requisição HTTP original aparece no log estruturado do consumidor.
- [x] Métrica `mensageria.dlq.eventos` visível em `/actuator/prometheus`.
- [x] `e2e/matricula-flow.sh` (estendido) cobre criar matrícula → evento processado → registro de
  idempotência criado.
- [x] README documenta como inspecionar filas/DLQ via RabbitMQ Management UI.
- [x] Cobertura ≥ 80% no módulo `notificacao` (92,9% de linha, ver `target/site/jacoco/jacoco.csv`).
- [x] `code-reviewer` e `security-auditor` executados — achados endereçados (ver seção 10 e D035): porta da
  Management UI restrita a loopback (estava aberta, inconsistente com todo outro console admin do
  `compose.yaml`), string literals de exchange/routing key substituídas por constantes de
  `MensageriaConfig`, comentário incorreto sobre trace ID em `application.properties` corrigido, risco de
  rede sem autenticação registrado explicitamente em D035 (antes só citado, não descrito).
- [x] `docs/DECISIONS.md` contém D032–D035 (achados de review incorporados dentro de D034/D035, sem inflar
  o log com entradas novas de baixo risco — mesmo padrão já usado no projeto).

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Extração de routing key/eventId, decisão de rotear para DLQ após N tentativas (mock do `x-death` header) | `notificacao/MatriculaNotificacaoListenerTest.java` |
| Integração | Outbox (reenvio no restart), retry→DLQ com TTL curto, idempotência (redelivery não duplica) | `notificacao/*IntegrationTest.java` (Testcontainers Postgres + RabbitMQ via Testcontainers) |
| E2E | Criar matrícula → evento publicado → consumidor processa → `evento_processado` criado | `e2e/matricula-flow.sh` (estendido) |

**Justificativa de cobertura:** os testes de integração são os que provam a confiabilidade real (outbox,
retry, idempotência) — exatamente o que este desafio pede como diferencial sênior de mensageria; testes
unitários cobrem só a lógica de decisão do consumidor (routing key, contagem de tentativas), que não
depende de infraestrutura real.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** log estruturado (WARN) quando uma mensagem vai para a DLQ; métrica
  `mensageria.dlq.eventos`; trace ID propagado até o consumidor (D034).
- **Segurança:** credenciais do RabbitMQ via `.env` (`RABBITMQ_DEFAULT_USER`/`RABBITMQ_DEFAULT_PASS`, já
  padronizadas na Fase 1), nunca hardcoded. Payload AMQP carrega só `matriculaId`/`alunoId`/`turmaId`/
  `liberouVaga`/`eventId` — nenhum dado sensível (email, nome) trafega na mensagem. **Risco conhecido, não
  mitigado nesta fase:** a rede do compose não exige autenticação adicional entre serviços (qualquer
  container na mesma rede Docker alcança o RabbitMQ com as credenciais do `.env`) — aceitável em
  desenvolvimento (rede isolada, não exposta externamente), mas em produção exigiria mTLS entre serviços ou
  rede segmentada por serviço.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando (129/129 testes do projeto, `./mvnw clean verify`)
- [x] Cobertura ≥ 80% no módulo `notificacao` (92,9%)
- [x] `docs/DECISIONS.md` atualizado com D032–D035 (achados de review incorporados nas próprias entradas)
- [x] `code-reviewer` executado — achados endereçados (ver seção 10)
- [x] `security-auditor` executado — achados endereçados (ver seção 10)
- [x] Validação manual ponta a ponta (publicar → falhar → retry → DLQ → reenvio manual sem duplicar)
  documentada nesta spec (seção 10)
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando

## 10. Validação manual

Executada contra a stack real via `docker compose` (Postgres/Redis/RabbitMQ/Keycloak + observabilidade) e a
aplicação rodando localmente (`./mvnw spring-boot:run`), usando a API HTTP do RabbitMQ Management UI
(`http://localhost:15672/api/...`, mesma porta agora restrita a loopback — achado de security review, ver
seção 6) como forma não-interativa de reproduzir exatamente o que um humano faria na aba **Publish
message**/**Get messages** da UI.

**1. Retry → DLQ (mensagem malformada, força falha de processamento):**

Publicada diretamente no exchange `gestao.eventos` com routing key `matricula.criada` uma mensagem com
payload JSON inválido (`{ isto nao e um json valido`). Acompanhamento da profundidade das filas a cada 2s:

```
t=2s  dlq=0 retry=1 main=0   (1ª falha: main -> retry, TTL 10s)
...
t=16s dlq=0 retry=1 main=0   (2ª falha: retry expirou -> main -> retry de novo)
t=18s dlq=1 retry=0 main=0   (3ª falha: consumidor esgotou tentativas, publicou explicitamente na DLQ)
```

Mensagem inspecionada na DLQ via `POST /api/queues/%2f/notificacao.matricula.dlq/get` (modo
`ack_requeue_true`, não-destrutivo — a mesma operação que a aba **Get messages** da UI faz):

```json
{
  "payload": "{ isto nao e um json valido",
  "properties": { "headers": {
    "retry-count": 2,
    "x-death": [
      {"count": 2, "queue": "notificacao.matricula.retry", "reason": "expired", "routing-keys": ["notificacao.matricula.retry"]},
      {"count": 2, "queue": "notificacao.matricula", "reason": "rejected", "routing-keys": ["matricula.criada"]}
    ],
    "x-first-death-queue": "notificacao.matricula",
    "x-first-death-reason": "rejected"
  }}
}
```

Confirma: (a) a mensagem passou por exatamente 3 tentativas (2 dead-letters registrados + a 3ª que disparou
o envio explícito à DLQ, D033); (b) o payload original é preservado intacto, inspecionável; (c) a routing
key original (`matricula.criada`) é recuperável via `x-death[1].routing-keys` mesmo após o bounce pela fila
de retry — exatamente o mecanismo implementado em `routingKeyOriginal()` (achado de teste de integração, ver
seção 4.4).

**2. Idempotência (reenvio manual não duplica):**

Criada uma matrícula real via API (`POST /api/matriculas`), aluno=5, turma=12, matrícula id=8. Consumidor
processou com sucesso (log: `"Notificação seria enviada aqui - matrícula 8 criada..."`) e gravou
`evento_processado` com `eventId=397fb779-6d69-42c7-a342-9d76154dde64`. Confirmado `SELECT COUNT(*) FROM
evento_processado WHERE id = '397fb779-...'` → `1`.

Reenviada manualmente a MESMA mensagem (mesmo `eventId`, simulando um reenvio via aba **Publish message** da
Management UI após uma redelivery/falha de ack) via `POST /api/exchanges/%2f/gestao.eventos/publish`.
Resultado:
- `SELECT COUNT(*) FROM evento_processado WHERE id = '397fb779-...'` → continua `1` (sem duplicar).
- Log do consumidor: `"Evento 397fb779-6d69-42c7-a342-9d76154dde64 (MATRICULA_CRIADA) já processado -
  ignorando redelivery (idempotência)."`, com `"traceId":"validacao-manual"` (o valor enviado na mensagem
  manual) — confirma também que o traceId embutido no payload chega corretamente ao MDC do consumidor
  mesmo numa mensagem publicada fora do fluxo HTTP normal.

**3. E2E automatizado:** `bash e2e/matricula-flow.sh` executado contra a stack real, todas as etapas OK,
incluindo a nova verificação de idempotência (`evento_processado` com ≥ 3 registros após o fluxo completo
criar→confirmar→cancelar, que publica 3 eventos).

**Conclusão:** as três garantias centrais desta fase (retry+DLQ nunca perde mensagem silenciosamente,
idempotência protege contra redelivery/reenvio manual, trace ID rastreia uma mensagem na DLQ de volta à
origem) foram verificadas contra infraestrutura real, não só em teste automatizado.
