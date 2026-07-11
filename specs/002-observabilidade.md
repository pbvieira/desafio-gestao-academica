# 002 — Observabilidade (métricas, tracing, logs estruturados)

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §04 (observabilidade mínima: logs estruturados, correlation/trace ID, health checks, métricas básicas), §07 (diferencial: tracing distribuído)
**Módulo(s) Modulith afetado(s):** N/A — infraestrutura/observabilidade transversal, anterior à criação dos módulos de domínio.

## 1. Objetivo

Instrumentar a aplicação com health checks, métricas (Prometheus), tracing distribuído (Jaeger via OTLP) e logs estruturados correlacionados por `traceId`/`spanId`, e disponibilizar essa telemetria via uma stack de observabilidade (Prometheus, Grafana, Jaeger, Loki+Promtail) no Docker Compose — sem ainda nenhuma métrica ou trace de regra de negócio, já que não há domínio implementado.

## 2. Escopo

**Dentro do escopo:**
- Adicionar ao `pom.xml`: `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`.
- Configurar Actuator: `/actuator/health` (com liveness/readiness), `/actuator/prometheus`; expor **apenas** os endpoints necessários (não usar `include=*` — ver seção 8).
- Configurar Micrometer Tracing com bridge OTel, exportando 100% das amostras via OTLP para o Jaeger.
- Logs estruturados em JSON (formato nativo do Spring Boot 3.4+, `logging.structured.format.console=ecs`), incluindo `trace_id`/`span_id` automaticamente via MDC.
- Adicionar ao `compose.yaml`, atrás de um profile opcional `observability` (D008): `prometheus`, `grafana` (com datasources provisionados: Prometheus, Jaeger, Loki — D007/D009), `jaeger` (`jaegertracing/all-in-one`, aceitando OTLP), `loki` + `promtail` (D007).
- Comentários no `compose.yaml` explicando o papel de cada serviço novo.
- Provisionar um dashboard básico (JVM/HTTP) no Grafana por padrão (D009).
- Validar ponta a ponta: gerar uma requisição real, confirmar a métrica no Prometheus, o trace no Jaeger, e o log da mesma requisição no Loki (via Grafana Explore) com o mesmo `traceId`.
- `code-reviewer` e `security-auditor` sobre toda a configuração nova, com atenção a exposição de endpoints do Actuator e credenciais do Grafana.

**Fora do escopo (não-objetivos):**
- Qualquer entidade, tabela, controller ou service de domínio acadêmico — Fase 2.
- Métricas ou traces de regra de negócio (ex: matrículas confirmadas/min) — não existem ainda; o dashboard e as métricas desta fase são genéricas (JVM, HTTP, Actuator).
- **Tracing propagando através das mensagens do RabbitMQ** — explicitamente pedido para ficar pendente; entra quando o producer/consumer de eventos de domínio existir de fato (Fase 3+). Registrado como item pendente na seção 4.
- `SecurityConfig` completo (RBAC/OAuth2 Resource Server) — fica para a spec 003. **Durante a implementação desta fase**, porém, foi necessário criar um `SecurityConfig` *mínimo e provisório* (`br.com.desafio.tecnico.gestao.security.config.SecurityConfig`), liberando só `/actuator/health/**` e `/actuator/prometheus` sem autenticação — sem ele, o `spring-boot-starter-security` (já presente desde a Fase 1) bloqueava `/actuator/prometheus` com autenticação básica default, impedindo o Prometheus de escrapar a aplicação. Esse stub será substituído/estendido pela configuração completa da spec 003.
- Centralizar no Loki os logs de **outros** containers (Postgres, Keycloak, RabbitMQ) além da própria aplicação — avaliado, implementado e depois **removido** por achado de security review (ver seção 4 e D007/D017): o mecanismo necessário para isso (montar `/var/lib/docker/containers` ou `docker.sock`) expõe segredos em texto claro de qualquer container do host. Só o log da própria app é centralizado nesta fase.
- Subir a própria aplicação no `compose.yaml` — continua adiado (D003); Prometheus precisa alcançar a app rodando no host (ver seção 4).
- Dashboards de negócio, alertas do Prometheus/Grafana (Alertmanager) — não fazem parte do mínimo do PRD.

## 3. Regras de negócio envolvidas

N/A — fase de observabilidade/infraestrutura. Nenhuma regra de negócio do PRD §02 é implementada aqui.

## 4. Abordagem técnica (como implementado)

**Aplicação Spring Boot:**
- `management.endpoints.web.exposure.include=health,prometheus` — expõe só o estritamente necessário (não `*`); `management.endpoint.health.show-details` permanece no default (`never`).
- `management.tracing.sampling.probability=1.0` — 100% de amostragem (ambiente de desenvolvimento/demo).
- `management.otlp.tracing.endpoint=http://localhost:${JAEGER_OTLP_HTTP_PORT:4318}/v1/traces` — nota de sintaxe: placeholders do Spring Boot usam **um só `:`** como separador de default (`${VAR:default}`), não o `${VAR:-default}` de shell; o bug (`${VAR:-4318}` resolvendo para a porta literal `"-4318"`, com hífen, e quebrando o cliente OTLP) foi cometido e corrigido durante a validação.
- `management.metrics.tags.application=${spring.application.name}` — adicionado durante a implementação (não estava na spec original) para dar às métricas do Prometheus um label `application="gestao"` estável, usado pelo dashboard provisionado (D009).
- `logging.structured.format.console=ecs` — formato JSON nativo do Spring Boot. **Nomes de campo reais, descobertos empiricamente** (divergem do que a spec original assumiu): o Micrometer Tracing grava `traceId`/`spanId` como chaves **planas** no nível raiz do JSON (não `trace.id`/`span.id` aninhados, como o ECS genérico sugeriria) — os arquivos de configuração do Promtail/Grafana foram ajustados para esses nomes reais.
- `logging.file.name=logs/gestao.log` + `logging.structured.format.file=ecs` — **adicionado durante a implementação**, não previsto originalmente (ver "ponte de logs" abaixo).
- `br.com.desafio.tecnico.gestao.security.config.SecurityConfig` (novo, mínimo/provisório) — permite `/actuator/health/**` e `/actuator/prometheus` sem autenticação; nega o resto (`anyRequest().authenticated()`). Ver seção 2.

**Descoberta durante a implementação — ponte de logs via arquivo (D017):** a aplicação roda no host, fora do compose (D003). O driver `json-file` do Docker só captura logs de *containers*, nunca de um processo do host — então o Promtail nunca via os logs da app. Resolvido fazendo a app também escrever em `logs/gestao.log` (mesmo formato ECS), montado somente-leitura no container do Promtail. `logs/` entrou no `.gitignore`.

**Prometheus (`docker/prometheus/prometheus.yml`):**
- Scrape job `gestao-app` apontando para `host.docker.internal:8080` (porta default do Spring Boot). `extra_hosts: ["host.docker.internal:host-gateway"]` no serviço `prometheus` do `compose.yaml` permite o container alcançar o host. Healthcheck via `wget --spider .../-/ready`.

**Jaeger:**
- Imagem `jaegertracing/all-in-one:1.60` (a tag `1.63` pedida inicialmente não existe no Docker Hub; verificado via `docker manifest inspect` antes de fixar a versão real). `COLLECTOR_OTLP_ENABLED=true`, portas `4317` (OTLP/gRPC), `4318` (OTLP/HTTP), `16686` (UI), todas em `127.0.0.1`. Healthcheck via `wget --spider` na UI.

**Loki + Promtail (D007, com refinamento pós-security-review — ver D017):**
- `loki`: imagem `grafana/loki:3.3.2`, config padrão (`docker/loki/loki-config.yaml`). Healthcheck via `/ready`.
- `promtail`: a proposta original (scrape estático de `/var/lib/docker/containers/*/*-json.log`, evitando `docker.sock`, para centralizar logs de **todos** os containers) foi implementada, mas o `security-auditor` apontou que o bind mount do diretório inteiro expõe `config.v2.json` de cada container — que contém `Config.Env` (segredos) em texto claro, de **qualquer** container do host, não só deste projeto. **Removido**: o Promtail hoje só lê `logs/gestao.log` (a ponte de arquivo da própria app, D017), nada mais. Também removidos `trace_id`/`span_id` do stage `labels:` do Promtail (outro achado: alta cardinalidade + vetor de esgotamento via endpoints públicos) — a correlação log→trace no Grafana usa `derivedFields` (regex sobre o *conteúdo* da linha), não precisa de label.
- Bug de configuração corrigido durante a validação: a imagem `grafana/promtail` não usa `/etc/promtail/config.yaml` por padrão (cai silenciosamente na config de exemplo embutida na imagem) — foi necessário `command: '-config.file=/etc/promtail/config.yaml'` explícito no `compose.yaml`.

**Grafana:**
- Provisionamento via arquivo: datasources (Prometheus, Jaeger, Loki, com `uid` explícito para permitir `derivedFields` estável) e um dashboard básico (JVM/HTTP, D009) — sem nenhuma configuração manual na UI.
- Credenciais via `${GRAFANA_ADMIN_USER:-admin}`/`${GRAFANA_ADMIN_PASSWORD:?...}` (usuário não é segredo, senha obrigatória sem default óbvio — mesmo padrão de D004).
- `depends_on` com `condition: service_healthy` em `prometheus`, `jaeger` e `loki` (achado de code review: `depends_on` "fraco", sem esperar o serviço ficar pronto, quebrava a consistência com o padrão já usado em `keycloak`→`postgres` na Fase 1).

**Docker Compose profiles (D008):**
- Os 5 serviços novos (`prometheus`, `grafana`, `jaeger`, `loki`, `promtail`) têm `profiles: ["observability"]`. `docker compose up` continua subindo só os 4 serviços de infra "core"; `docker compose --profile observability up` sobe tudo.

**Validação ponta a ponta (real, não simulada):** uma requisição a `/actuator/health` gerou o mesmo `traceId` confirmado em: Prometheus (métrica `jvm_memory_used_bytes{application="gestao"}` com o scrape target `up`), Jaeger (trace real com 5 spans, incluindo `secured request`/`authorize request` do `SecurityConfig`), e Loki (log da mesma requisição, via `{job="gestao-app"}`, contendo o `traceId` no conteúdo da linha).

## 5. Decisões que requerem confirmação antes de implementar

Decisões estratégicas já levantadas e registradas antes desta implementação começar:

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Backend de log centralizado? | Loki+Promtail vs. EFK/ELK | [D007](../docs/DECISIONS.md#d007) — Loki+Promtail |
| 2 | Stack de observabilidade sempre de pé ou opcional? | Sempre junto vs. Compose profile opcional | [D008](../docs/DECISIONS.md#d008) — profile `observability` |
| 3 | Grafana já vem com dashboard, ou só datasources? | Só datasources vs. dashboard básico JVM/HTTP | [D009](../docs/DECISIONS.md#d009) — dashboard básico |

A decisão de implementação sobre scrape estático vs. `docker.sock` no Promtail foi **superada pela implementação real**: nenhuma das duas foi mantida para *outros* containers — só a ponte de arquivo da própria app (D017), após achado de security review (ver seção 4).

## 6. Critérios de aceite

- [x] `docker compose --profile observability up` sobe prometheus, grafana, jaeger, loki e promtail sem erro (todos `healthy`); `docker compose up` (sem profile) continua subindo só os 4 serviços de infra core.
- [x] `/actuator/health` e `/actuator/prometheus` acessíveis na aplicação; nenhum outro endpoint do Actuator exposto (confirmado via `SecurityConfig` mínimo, ver seção 2).
- [x] Uma requisição HTTP real (`/actuator/health`) gerou: métrica visível no Prometheus (`jvm_memory_used_bytes{application="gestao"}`, target `up`), trace visível no Jaeger (5 spans), log visível no Loki (`{job="gestao-app"}`) — **os três com o mesmo `traceId`** (`9e101734f6e76279eb6fcd64a95f7834`, verificado nos três sistemas).
- [x] Logs da aplicação saem em JSON estruturado (ECS), com `traceId`/`spanId` presentes quando há uma requisição em andamento (nomes de campo reais, não `trace_id`/`span_id` como assumido originalmente).
- [x] Grafana com datasources (Prometheus, Jaeger, Loki) e o dashboard básico já provisionados, sem passo manual na UI (confirmado via API do Grafana).
- [x] Nenhuma credencial hardcoded (Grafana incluso) — tudo via variável de ambiente obrigatória, sem default óbvio.
- [x] `code-reviewer` executado — achados endereçados: healthchecks/`depends_on` reforçados, acentuação corrigida, teste de `/actuator/health` adicionado, comentários de acoplamento (porta do Jaeger, porta da app no Prometheus, `job` vs. `application`).
- [x] `security-auditor` executado — achados endereçados: removido o job que centralizava logs de outros containers (expunha `Config.Env`/segredos via `config.v2.json`), removidos `trace_id`/`span_id` como labels do Loki (alta cardinalidade + vetor de esgotamento via endpoint público).
- [x] `docs/DECISIONS.md` contém D007–D009 e D017 (decisão emergente durante a implementação, com refinamentos pós-review).
- [x] Esta spec atualizada para refletir o que foi de fato implementado.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | N/A — não há lógica de negócio nesta fase | — |
| Integração | `/actuator/health` público e retornando `UP` | `src/test/java/br/com/desafio/tecnico/gestao/ActuatorHealthIntegrationTest.java` (novo) |
| E2E | N/A nesta fase — a validação ponta a ponta (métrica/trace/log correlacionados) é manual, conforme seção 6, não automatizada | — |

**Justificativa de cobertura:** como na Fase 1, não há regra de negócio aqui — a validação relevante é operacional (o pipeline de telemetria funciona ponta a ponta), verificada por execução real, não por métrica de cobertura de linha.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** esta fase *é* a introdução da observabilidade — logs estruturados (JSON/ECS), métricas via Prometheus, tracing via Jaeger, tudo correlacionado por `traceId`.
- **Segurança:**
  - Endpoints do Actuator: exposição explícita e mínima (`health,prometheus`), nunca `include=*` — `/actuator/env`, `/actuator/beans`, `/actuator/heapdump` etc. permanecem inacessíveis. O `SecurityConfig` mínimo criado nesta fase nega tudo o mais por padrão (`anyRequest().authenticated()`); será substituído pela configuração real de RBAC na spec 003.
  - Credenciais do Grafana obrigatórias via variável de ambiente, sem fallback óbvio, mesmo padrão de D004/refinamento de segurança da Fase 1.
  - **Achado de security review, corrigido:** a primeira implementação montava `/var/lib/docker/containers` (todo o diretório) no Promtail para centralizar logs de todos os containers, evitando `docker.sock` — mas isso expõe `config.v2.json` (com `Config.Env`, segredos em texto claro) de **qualquer** container do host. Removido; o Promtail só lê o log da própria aplicação agora (D017).
  - **Achado de security review, corrigido:** `trace_id`/`span_id` promovidos a labels do Loki criavam um vetor de esgotamento de recursos (alta cardinalidade, amplificável via `/actuator/health`/`/actuator/prometheus` sem autenticação). Removidos dos labels; a correlação log→trace no Grafana usa `derivedFields` sobre o conteúdo da linha, sem precisar de label.
  - Todas as portas novas (Prometheus `9090`, Grafana `3000`, Jaeger `16686`/`4317`/`4318`, Loki `3100`) expostas só em `127.0.0.1`, mesmo padrão do Keycloak (D004) — confirmado pelo security review, sem achado.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando (`ActuatorHealthIntegrationTest`, adicionado durante a implementação — achado de code review)
- [x] Cobertura ≥ 80% no(s) módulo(s) afetado(s), com sentido — N/A nesta fase (sem lógica de negócio); ver justificativa na seção 7
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D007–D009, D017)
- [x] `code-reviewer` executado — achados endereçados
- [x] `security-auditor` executado — achados endereçados
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando (confirmado localmente, exit code 0)
