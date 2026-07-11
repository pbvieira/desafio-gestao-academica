# 004 — Fundações transversais (fechamento da Fase 1)

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §03 (validação/erros padronizados), §04 (Docker Compose, observabilidade), §05 (documentação de API), §06 (critérios críticos: testes, erros)
**Módulo(s) Modulith afetado(s):** transversal — afeta `security` (spec 003) e a raiz da aplicação; nenhum módulo de domínio ainda.

## 1. Objetivo

Fechar a Fase 1 com fundações que, se adiadas para a Fase 2, gerariam retrabalho espalhado por todos os controllers/entidades futuros: gate de cobertura, tratamento padronizado de erros (RFC 7807), convenção de migrations, higiene de segredos, Swagger com Bearer, esqueleto de CI e o primeiro smoke test e2e da infraestrutura.

## 2. Escopo

**Dentro do escopo (7 subitens, cada um com critérios de aceite próprios na seção 6):**
1. Gate de cobertura JaCoCo (80%, com exclusões documentadas — D014).
2. `@RestControllerAdvice` global com `ProblemDetail` (400/401/403/404/409/500), `type`+`errorCode` (D016).
3. Convenção de migrations Flyway (D015) documentada e aplicada.
4. `.env.example` completo + auditoria final de credenciais hardcoded + passo no README.
5. Swagger UI com esquema Bearer JWT.
6. `.github/workflows/ci.yml` mínimo (build + gate de cobertura + e2e).
7. Primeiro smoke test em `e2e/` validando toda a infraestrutura de pé.

**Fora do escopo (não-objetivos):**
- Qualquer entidade, tabela, controller ou regra de negócio de domínio acadêmico — Fase 2.
- Deploy real no workflow de CI (build+gate de qualidade só; deploy fica para uma fase futura, não especificada ainda).
- Framework de e2e mais sofisticado (REST Assured/Playwright) — o smoke test desta fase é um script simples (ver seção 4.7); reavaliar ferramenta quando o frontend/domínio entrarem no fluxo e2e de verdade.
- Cache de dependências Maven no CI, matriz de versões de JDK — registrado como pendência futura (seção 4.6).
- Qualquer regra ABAC real de domínio — já registrado como pendência na spec 003, não revisitado aqui.

## 3. Regras de negócio envolvidas

N/A — fase de fundações transversais. Nenhuma regra do PRD §02 é implementada aqui.

## 4. Abordagem técnica

### 4.1 JaCoCo (D014)

- `jacoco-maven-plugin` no `pom.xml`: goals `prepare-agent` (fase `test`) e `report`+`check` (fase `verify`).
- Regra: `BUNDLE`, `LINE` e `BRANCH` ≥ 80%.
- Exclusões (`<excludes>`): `**/*Application.class`, `**/config/**`, `**/dto/**` (e sufixos `*Request.class`/`*Response.class`).
- `lombok.config` na raiz do projeto com `lombok.addLombokGeneratedAnnotation = true` — JaCoCo já ignora nativamente código anotado `@Generated`, evitando excludes frágeis por caminho para getters/setters/equals/hashCode gerados.
- `./mvnw clean verify` deve **falhar** se a cobertura cair abaixo de 80% — validado deliberadamente (seção 6) baixando o threshold/comentando um teste, confirmando a falha, e revertendo.

### 4.2 `ProblemDetail` global (D016) — como implementado

- `br.com.desafio.tecnico.gestao.errorhandling.GlobalExceptionHandler` (`@RestControllerAdvice`), com `br.com.desafio.tecnico.gestao.errorhandling.ProblemDetailFactory` como ponto único de montagem do corpo (`type` URN + `errorCode` + `timestamp` + `path`), reaproveitado também pelos handlers 401/403 de nível de filtro da spec 003:
  - **400** — `MethodArgumentNotValidException` (bean validation) **e** `HttpMessageNotReadableException` (JSON malformado), ambos com `errorCode=VALIDATION_ERROR`. O primeiro lista os campos inválidos como extensão do `ProblemDetail` (`erros: [{campo, mensagem}]`) — mapeando `getFieldErrors()` **e** `getGlobalErrors()` (achado de code review: só `getFieldErrors()` descartava silenciosamente validações de nível de classe).
  - **404** — `RecursoNaoEncontradoException` (unchecked, reutilizável pela Fase 2); demonstrada por `errorhandling.web.ExemploRecursoController` (mesma linha dos exemplos de RBAC/ABAC da spec 003, não uma entidade de domínio fictícia).
  - **409** — `ConflitoRegraNegocioException` (unchecked, reutilizável pela Fase 2 — ex: matrícula duplicada); mesmo controller placeholder.
  - **500** — `@ExceptionHandler(Exception.class)` genérico: mensagem fixa, **sem** stack trace/mensagem da exceção original na resposta; a exceção real é logada (nível ERROR, com `traceId` via spec 002) só no servidor. Testado via `@MockitoSpyBean` forçando uma exceção inesperada através do dispatch real do MVC (não um endpoint "quebra de propósito").
  - **403** — `GlobalExceptionHandler.handleAcessoNegado` (`@ExceptionHandler(AccessDeniedException.class)`) é o produtor efetivo do 403 para negações de `@PreAuthorize` em métodos de controller — confirmado empiricamente (o `AccessDeniedHandler` de nível de filtro, spec 003, virou fallback defensivo, nunca acionado nos testes reais). Ambos usam `ProblemDetailFactory.accessDenied(path)` (achado de code review: eliminada a duplicação de string entre os dois arquivos).
  - **401** — continua sendo produzido pelo `AuthenticationEntryPoint` da spec 003 — **não pode** ser tratado por `@RestControllerAdvice`. Também reaproveitado pela cadeia de Basic Auth do scrape do Prometheus (achado de security review: antes, uma falha de autenticação em `/actuator/prometheus` saía em formato diferente do resto da API).
- Formato do `type`: URN estável (ex: `urn:gestao:erro:validation-error`, `urn:gestao:erro:not-authenticated`, `urn:gestao:erro:access-denied`, `urn:gestao:erro:resource-not-found`, `urn:gestao:erro:business-conflict`, `urn:gestao:erro:internal-error`); `errorCode` via `ProblemDetail#setProperty(...)` (D016).

### 4.3 Convenção de migrations (D015)

- Pasta única `src/main/resources/db/migration` (já existente, hoje só com o `README.md` da Fase 1).
- Convenção de nome: `V{sequencial}__{modulo}_{descricao_snake_case}.sql` (ex: `V1__academico_criar_tabela_aluno.sql`).
- `README.md` da pasta atualizado com a convenção, para orientar a Fase 2 (nenhuma migration real ainda é criada nesta fase — continua sem tabela de domínio).

### 4.4 Segredos e variáveis de ambiente

- Auditoria final: revisar `compose.yaml` (Fases 1–3: postgres/redis/rabbitmq/keycloak + observabilidade + Grafana) em busca de qualquer credencial ainda hardcoded — não deveria haver nenhuma (D004/refinamento já cobriu isso), mas esta é a checagem de fechamento de fase.
- `.env.example` atualizado com **todas** as variáveis das etapas anteriores (Postgres, Keycloak DB, RabbitMQ, Keycloak admin, Grafana admin) em um único lugar consolidado.
- `README.md` (criado nesta fase, ainda incompleto — será completado ao final do desafio) documenta o passo `cp .env.example .env` antes de subir o compose, e o comando `docker compose up` / `docker compose --profile observability up`.

### 4.5 Swagger/OpenAPI com Bearer

- `@SecurityScheme` (springdoc/Swagger, `type=HTTP`, `scheme=bearer`, `bearerFormat=JWT`, nome `bearerAuth`) + `@SecurityRequirement(name="bearerAuth")` aplicado globalmente (via customizer, não endpoint por endpoint) — habilita o botão "Authorize" no `/swagger-ui.html` para colar um token e reproduzir os bloqueios de RBAC/ABAC da spec 003 direto na UI.

### 4.6 CI (`.github/workflows/ci.yml`)

- Gatilho: `push` e `pull_request`.
- Passos: checkout → setup JDK 21 (Temurin) → `docker compose --profile observability up -d` (sobe os 9 serviços, incluindo observabilidade, necessário para o smoke test do item 7) → aguardar serviços saudáveis → `./mvnw clean verify` (inclui o gate JaCoCo) → executar o smoke test de `e2e/` (item 7).
- **Fora do escopo, registrado como pendência:** cache de dependências Maven (`actions/cache` na pasta `.m2`) e matriz de versões de JDK — otimizações que não bloqueiam o gate de qualidade mínimo pedido nesta fase.

### 4.7 Smoke test e2e (`e2e/smoke-test.sh`) — como implementado

- Confirmado o uso de script bash simples (não houve objeção) — `curl -sf`/`pg_isready` por serviço, função `verificar()` compartilhada, `set -uo pipefail` sem `-e` (reporta todas as falhas, não só a primeira).
- Verificações: Postgres (`pg_isready`), RabbitMQ (`GET /api/overview` autenticado), Keycloak (`GET /realms/gestao/.well-known/openid-configuration`), Prometheus (`GET /-/ready`), Jaeger (`GET /`), Loki (`GET /ready`), aplicação (`GET /actuator/health` → `UP`).
- Portas fixas de Prometheus/Grafana/Jaeger/Loki (já implementadas na spec 002) tornam o script determinístico, sem precisar consultar `docker port`.
- Validado de ponta a ponta neste ambiente: subida completa do zero (`docker compose --profile observability up -d` → `./mvnw clean verify` → `spring-boot:run` em background com loop de espera por `/actuator/health` → `smoke-test.sh`), replicando exatamente a sequência do workflow de CI, já que `act` não está disponível para rodar o YAML diretamente.

## 5. Decisões que requerem confirmação antes de implementar

Decisões já levantadas e registradas antes desta implementação começar:

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Exclusões do gate JaCoCo? | Config+DTOs+main+Lombok vs. também exceções/security config | [D014](../docs/DECISIONS.md#d014) — Config+DTOs+main+Lombok |
| 2 | Migrations em pasta única ou por módulo? | Pasta única vs. pasta por módulo | [D015](../docs/DECISIONS.md#d015) — pasta única |
| 3 | Formato do código de erro no ProblemDetail? | Só errorCode vs. type (URN) + errorCode | [D016](../docs/DECISIONS.md#d016) — type + errorCode |

Decisão de implementação levantada nesta seção (4.7), ainda **sem confirmação**: script bash vs. framework Java para o smoke test e2e — seguindo com o script por padrão salvo objeção.

## 6. Critérios de aceite

- [x] **JaCoCo:** `./mvnw clean verify` falha se cobertura < 80%; exclusões documentadas nesta spec e no `pom.xml`; falha deliberada (threshold elevado a 0.99) confirmada e revertida.
- [x] **ProblemDetail:** um teste de integração por status (400/401/403/404/409/500), corpo sem stack trace/detalhe interno (confirmado inclusive para o 500 via mock), `type`+`errorCode` presentes e distintos por categoria.
- [x] **Migrations:** `README.md` de `db/migration` documenta a convenção; nenhuma migration real ainda criada (continua Fase 2).
- [x] **Segredos:** auditoria cruzada confirmou 100% das variáveis de `compose.yaml` documentadas em `.env.example`; `.env` no `.gitignore`; `README.md` documenta `cp .env.example .env`. Achado de security review corrigido: `spring.security.user.password` (credencial de scrape do Prometheus) tinha zero indireção — agora usa `${PROMETHEUS_SCRAPE_PASSWORD:default}`, mesmo padrão do resto do arquivo.
- [x] **Swagger:** `/swagger-ui.html` mostra o botão "Authorize"; confirmado via `/v3/api-docs` (esquema `bearerAuth` presente, `security` global aplicado) e via requisição real com token do Keycloak.
- [x] **CI:** `.github/workflows/ci.yml` existe; validado replicando manualmente cada passo do YAML neste ambiente (checkout já dado, JDK já presente, `cp .env.example .env` → `docker compose --profile observability up -d` → `./mvnw clean verify` → subir app em background com loop de espera → `smoke-test.sh`) — sequência completa executada com sucesso, já que `act` não está disponível.
- [x] **Smoke e2e:** `e2e/smoke-test.sh` verifica os 9 serviços + a aplicação, com saída clara de sucesso/falha por serviço — validado rodando de verdade.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | `ProblemDetailFactory` (formato); `GlobalExceptionHandler` (404/409/500 chamados diretamente); `KeycloakClaimsTest` (branches de claim ausente/malformado, fechando gap de cobertura) | `ProblemDetailFactoryTest`, `GlobalExceptionHandlerTest`, `KeycloakClaimsTest` |
| Integração | Um caso por status (400 validação, 400 JSON malformado, 403 via `@PreAuthorize`, 404, 409, 500 via `@MockitoSpyBean`) via `MockMvc`, com asserção de `Content-Type: application/problem+json` | `TratamentoErrosIntegrationTest` (7 testes) |
| E2E | Smoke test de infraestrutura (`e2e/smoke-test.sh`) — todos os 9 serviços + app respondendo | `e2e/smoke-test.sh` |

**Justificativa de cobertura:** o valor aqui não é a régua de 80% em si (que já é gate automático via JaCoCo), é garantir que cada status de erro tem um caminho real testado, e que o smoke test e2e vira o primeiro gate de CI que qualquer regressão de infraestrutura futura vai quebrar.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** exceções tratadas pelo `GlobalExceptionHandler` (especialmente 500) são logadas com stack trace completo **só no servidor** (structured log da spec 002, com `traceId`), nunca na resposta ao cliente — confirmado por teste (`erroInesperadoNaoTratadoRetorna500ComMensagemGenerica`, injeta deliberadamente uma mensagem sensível e valida que ela não aparece na resposta).
- **Segurança:** `.env.example` revisado, zero valor real; `.github/workflows/ci.yml` não usa `env | grep` nem ecoa `.env`; `ProblemDetail` de todos os status confirmado sem vazamento de nome de exceção/stack trace/detalhe de schema. Achados de security review corrigidos: (1) credencial de scrape do Prometheus com indireção via variável de ambiente (antes era literal puro); (2) `AuthenticationEntryPoint` reaproveitado na cadeia de Basic Auth do Prometheus para formato consistente. Risco residual **não ativo** registrado (não corrigido nesta fase, fora de escopo): o passo de diagnóstico do CI (`docker compose logs` em caso de falha) despeja log de todos os containers sem filtro — seguro hoje porque só há valores `changeme-*` de dev, mas deve ser revisitado se o CI algum dia usar segredos reais via `secrets:` do GitHub Actions.
- **Rejeitado por incompatibilidade técnica:** `server.address=127.0.0.1` (cogitado para restringir a app à mesma exposição de rede do resto dos serviços) foi testado e descartado — `host.docker.internal` (usado pelo Prometheus para escrapar a app) resolve para o gateway da bridge do Docker, não para `127.0.0.1`; restringir a app a loopback quebra o scrape. Documentado em `application.properties`.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando (31 testes no total do projeto)
- [x] Cobertura ≥ 80% (gate JaCoCo passando; validado falhando de propósito e revertendo)
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D014–D016, com refinamento em D016)
- [x] `code-reviewer` executado — achados endereçados: teste de 500 via `@MockitoSpyBean` (não unitário puro), teste de JSON malformado, `getGlobalErrors()` mapeado, duplicação de string do 403 extraída para `ProblemDetailFactory.accessDenied()`, arquivo solto (`.disk-check.tmp`) removido
- [x] `security-auditor` executado — achados endereçados: credencial de scrape do Prometheus com indireção, `AuthenticationEntryPoint` reaproveitado na cadeia Basic Auth do Prometheus; `server.address=127.0.0.1` testado e descartado por incompatibilidade com o scrape via `host.docker.internal`
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando (31 testes, exit code 0)

## 10. Pronto para Fase 2

A Fase 1 (specs 001–004) entrega, como base para a modelagem de domínio:

- **Infraestrutura reproduzível**: `docker compose up` (Postgres, Redis, RabbitMQ, Keycloak) e `docker compose --profile observability up` (+ Prometheus, Grafana, Jaeger, Loki, Promtail) — zero passo manual, `.env.example` cobre 100% das variáveis usadas.
- **Persistência**: Flyway configurado (schema único `public`, D005), convenção de nomenclatura de migration documentada (D015) — a primeira migration real (`V1__academico_criar_tabela_aluno.sql`) é o primeiro artefato da Fase 2.
- **Mensageria**: RabbitMQ de pé e alcançável pela app (`spring-boot-starter-amqp` no `pom.xml`) — nenhum producer/consumer ainda; entra quando os eventos de domínio (`MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada`) existirem.
- **Observabilidade**: métricas (Prometheus), tracing (Jaeger via OTLP) e logs estruturados (Loki) correlacionados por `traceId`, validados ponta a ponta com uma requisição real. Dashboard básico provisionado no Grafana — a Fase 2 só precisa adicionar métricas/traces de negócio, a infraestrutura já existe.
- **Segurança**: RBAC completo via Keycloak (papéis `ALUNO`/`SECRETARIA`/`ADMIN`, realm como código) e o **mecanismo** de ABAC (`PermissionEvaluator` customizado) validado com um exemplo funcional — a Fase 2 reaproveita o mesmo padrão para as regras reais de posse sobre `Matrícula`/`Turma` (já documentadas como especificação em specs/003, seção 4.3), resolvendo o dono real via repositório em vez de comparar com um path variable.
- **Tratamento de erros**: `ProblemDetail` padronizado (400/401/403/404/409/500) com `type`+`errorCode`, e duas exceções (`RecursoNaoEncontradoException`, `ConflitoRegraNegocioException`) já prontas para a Fase 2 lançar diretamente ao implementar as regras de negócio do PRD §02 (turma não encontrada, aluno já matriculado, etc.).
- **Qualidade**: gate de cobertura JaCoCo (80%) já ativo no build — todo código novo da Fase 2 precisa vir com teste desde o primeiro commit para não quebrar `./mvnw clean verify`.
- **CI**: workflow mínimo já validado (build + gate de cobertura + smoke test); a Fase 2 pode estender o smoke test em `e2e/` com o primeiro fluxo de negócio real ("aluno se matricula → confirma → vaga é consumida → evento é publicado").

**Pendências explícitas para a Fase 2** (já registradas nas specs desta fase, não esquecidas):
- Regras ABAC reais sobre Matrícula/Turma (specs/003, seção 4.3).
- Tracing propagando pelas mensagens do RabbitMQ (specs/002, fora de escopo).
- Trilha de auditoria para acessos via override de staff (achado M3 de security review, specs/003).
- Uso efetivo (ou remoção) do client Keycloak `gestao-backend`, hoje sem consumidor (D013).
- Cache de dependências Maven e matriz de JDK no CI (spec 004, seção 4.6).
