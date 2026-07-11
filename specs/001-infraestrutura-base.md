# 001 — Infraestrutura base (Docker Compose, Flyway, autenticação)

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §03 (Persistência, Ambiente), §04 (mensageria, Docker Compose Sênior), §07 (diferencial: autenticação e autorização)
**Módulo(s) Modulith afetado(s):** N/A nesta fase — trabalho de infraestrutura transversal, anterior à criação dos módulos de domínio (`academico`, `notificacao`).

## 1. Objetivo

Estabelecer a infraestrutura de suporte do projeto — banco de dados, cache/sessão, mensageria e identidade — via Docker Compose e a estrutura inicial de migrations Flyway, sem nenhum código ou tabela de domínio acadêmico, preparando o terreno para as fases de implementação de domínio (Fase 2 em diante).

## 2. Escopo

**Dentro do escopo:**
- Adicionar RabbitMQ ao `compose.yaml`, com plugin de management habilitado.
- Adicionar Keycloak ao `compose.yaml`, usando um banco `keycloak` dedicado no mesmo container Postgres (D004), criado via script de init do Postgres.
- Criar a estrutura `src/main/resources/db/migration` e a configuração do Flyway (schema único `public`, D005).
- Garantir que nenhuma credencial fica hardcoded no `compose.yaml` — tudo vem de variáveis de ambiente, com um `.env.example` versionado documentando as chaves esperadas.
- Validar a subida real via `docker compose up`: postgres, redis, rabbitmq e keycloak sobem sem erro.
- Validar que a aplicação (`./mvnw spring-boot:run`, rodando fora do compose) conecta com sucesso em postgres, redis e rabbitmq.
- Rodar `code-reviewer` e `security-auditor` sobre tudo o que foi criado (compose, migrations, config de Keycloak) antes de considerar a fase concluída.

**Fora do escopo (não-objetivos):**
- Qualquer entidade, tabela, controller ou service de domínio acadêmico (Aluno, Curso, Disciplina, Turma, Matrícula) — Fase 2.
- Configuração fina de realm/client do Keycloak (roles, mappers de domínio, fluxo de login completo) além do mínimo para o serviço subir e a app apontar para ele como issuer OAuth2 — detalhamento fica para quando endpoints de domínio existirem e precisarem de autenticação/autorização de fato.
- Subir a própria aplicação no `compose.yaml` — adiado para uma fase posterior (D003).
- Tabela ou coluna de mapeamento Keycloak ↔ Aluno — adiado para a Fase 2 (D006).
- Outbox Pattern e idempotência de consumo de mensagens — são diferenciais do PRD §07, não fazem parte da infraestrutura mínima desta fase.
- Qualquer migration de tabela de domínio ou tabela "de apoio" inventada apenas para preencher escopo — se, ao implementar, nenhuma tabela auxiliar concreta for necessária, a primeira migration existe só para validar a estrutura/config do Flyway (ver seção 4).
- **Confirmado na implementação:** nenhuma tabela auxiliar/de configuração concreta foi identificada como necessária nesta fase — `src/main/resources/db/migration` existe só com um `README.md` explicando por quê, e o Flyway roda contra ele sem nenhuma migration real (ver seção 4).
- **Adicionado durante a implementação (não estava no escopo original):** apontar a aplicação para o Keycloak como issuer OAuth2 (mencionado na primeira versão desta seção) foi explicitamente adiado — nenhum realm/client foi provisionado nesta fase (isso está, corretamente, fora do escopo), então uma `issuer-uri` apontando para um realm inexistente seria uma configuração morta. Isso entra junto da fase que criar o realm/client e o `SecurityConfig` de fato.

## 3. Regras de negócio envolvidas

N/A — esta é uma fase exclusivamente de infraestrutura. Nenhuma regra de negócio do PRD §02 é implementada aqui.

## 4. Abordagem técnica

**Docker Compose (`compose.yaml`) — como implementado:**
- `rabbitmq`: imagem `rabbitmq:4.1-management` (pinada, não flutuante); porta AMQP (`5672`) efêmera (auto-descoberta pelo `spring-boot-docker-compose`, igual postgres/redis); porta da management UI fixada via `${RABBITMQ_MANAGEMENT_PORT:-15672}` (uso humano); healthcheck via `rabbitmq-diagnostics ping`. Usuário via `RABBITMQ_DEFAULT_USER` (default `guest`, não é segredo); senha via `RABBITMQ_DEFAULT_PASS`, **obrigatória** (`${VAR:?...}`, sem fallback adivinhável — ver refinamento de D004/segurança abaixo).
- `keycloak`: imagem `quay.io/keycloak/keycloak:26.0` (pinada), modo `start-dev`; `KC_BOOTSTRAP_ADMIN_USERNAME`/`KC_BOOTSTRAP_ADMIN_PASSWORD` (nomes corretos e não-depreciados da v26); `KC_DB_URL` aponta para o banco `keycloak` dedicado no serviço `postgres`; `KC_DB_USERNAME`/`KC_DB_PASSWORD` usam uma role própria (`keycloak_app`), não mais o superusuário da aplicação (ver abaixo). Porta HTTP fixada via `${KEYCLOAK_HTTP_PORT:-8081}`, mas **vinculada só a `127.0.0.1`** (não exposta em outras interfaces). `depends_on: postgres (service_healthy)`.
- `postgres`: healthcheck via `pg_isready`; volume de script de init (`./docker/postgres-init/01-create-keycloak-db.sh`) que cria uma **role dedicada `keycloak_app`** (via `KEYCLOAK_DB_USER`/`KEYCLOAK_DB_PASSWORD`) e o banco `keycloak` com essa role como dono — implementa D004 com segregação de privilégio (não só de dados).
- Todas as senhas (Postgres, RabbitMQ, Keycloak admin, Keycloak DB) são obrigatórias via `${VAR:?mensagem}` — sem fallback para valores óbvios (`guest`, `admin`, `secret`); usuários (não-secretos) mantêm defaults de conveniência. `.env.example` documenta todas as chaves com valores de exemplo não-triviais; `.env` real fica no `.gitignore`.

  > **Refinamento pós-review (ver D004 em DECISIONS.md):** a primeira versão desta implementação reaproveitava `POSTGRES_USER`/`POSTGRES_PASSWORD` como credencial do Keycloak e usava `guest`/`admin` como fallback de senha. O `security-auditor` apontou que isso (a) quebrava a segregação de privilégio pretendida por D004 e (b) contrariava o próprio critério da seção 8 desta spec ("nenhuma delas deve ficar acessível com credenciais default/óbvias"). Corrigido antes do fechamento da fase.

**Aplicação Spring Boot:**
- Adicionada a dependência `spring-boot-starter-amqp` ao `pom.xml` (necessária para a auto-configuração do RabbitMQ pelo `spring-boot-docker-compose`; sem ela a app não teria como conectar no broker).
- `spring.docker.compose.skip.in-tests=false` adicionado a `application.properties` — por padrão o Spring Boot desativa a integração com o Docker Compose durante testes; como o único teste desta fase (`contextLoads`) depende dos serviços reais estarem no ar (conforme já previsto na seção 7 original desta spec), essa integração precisou ser explicitamente reativada para testes. Necessário para `./mvnw clean verify` passar.
- Nenhuma configuração de OAuth2 resource-server/issuer foi adicionada — ver nota em "Fora do escopo" (seção 2) sobre por quê isso foi adiado.

**Flyway:**
- `src/main/resources/db/migration` criado, schema único `public` (D005), sem locations múltiplas por módulo.
- Contém apenas um `README.md` explicando a ausência de migrations nesta fase — nenhuma tabela de negócio nem tabela "genérica de parâmetros" foi criada, por não haver necessidade concreta identificada (confirmado na seção 2). O Flyway roda normalmente contra a location vazia, criando `flyway_schema_history` no schema `public` (validado via `./mvnw spring-boot:run` e via `./mvnw clean verify`).

## 5. Decisões que requerem confirmação antes de implementar

Todas as decisões não-triviais identificadas para esta spec já foram levantadas e registradas antes desta implementação começar:

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | RabbitMQ ou Kafka para mensageria? | RabbitMQ vs. Kafka | [D002](../docs/DECISIONS.md#d002) — RabbitMQ |
| 2 | A aplicação sobe no compose já nesta fase? | Já na Fase 1 vs. fase posterior | [D003](../docs/DECISIONS.md#d003) — fase posterior |
| 3 | Como isolar o banco do Keycloak? | Schema separado no mesmo DB vs. DB dedicado no mesmo container vs. container Postgres dedicado | [D004](../docs/DECISIONS.md#d004) — DB dedicado no mesmo container |
| 4 | Schema Postgres por módulo ou único? | Schema único `public` vs. schema por módulo | [D005](../docs/DECISIONS.md#d005) — schema único |
| 5 | Mapeamento Keycloak↔Aluno entra nesta fase? | Já nesta fase (tabela solta) vs. adiado para a Fase 2 | [D006](../docs/DECISIONS.md#d006) — adiado para Fase 2 |

## 6. Critérios de aceite

- [x] `docker compose up` sobe `postgres`, `redis`, `rabbitmq` e `keycloak` sem erros.
- [x] RabbitMQ management UI acessível na porta exposta, com plugin de management ativo.
- [x] Keycloak acessível via HTTP e usando o banco `keycloak` dedicado (comprovável nos logs de startup do Keycloak e confirmado via `\dt` no banco `keycloak`).
- [x] Nenhuma credencial hardcoded em `compose.yaml` — todas vêm de variáveis de ambiente; `.env.example` documenta as chaves esperadas. Reforçado após security review: senhas não têm mais fallback óbvio (`${VAR:?...}` obrigatório).
- [x] `./mvnw spring-boot:run` conecta com sucesso em postgres, redis e rabbitmq, sem erro de conexão nos logs.
- [x] `src/main/resources/db/migration` criada e reconhecida pelo Flyway no startup (log de criação/validação da tabela de histórico do schema).
- [x] `code-reviewer` executado sobre o diff — achados endereçados (ver seção 4 e histórico de commits).
- [x] `security-auditor` executado sobre o diff — achados endereçados: role dedicada para o Keycloak, senhas obrigatórias sem fallback óbvio, porta do Keycloak restrita a `127.0.0.1`.
- [x] `docs/DECISIONS.md` contém D002–D006, todas já registradas (D004 com addendum pós-review).
- [x] Esta spec atualizada para refletir o que foi de fato implementado.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | N/A — não há lógica de negócio nesta fase | — |
| Integração | Contexto Spring sobe com sucesso apontando para os serviços reais do compose (datasource, Redis, RabbitMQ) | `src/test/java/br/com/desafio/tecnico/gestao/GestaoApplicationTests.java` (estendido, se necessário) |
| E2E | N/A — não há fluxo de usuário nesta fase | — |

**Justificativa de cobertura:** como não há regra de negócio nem endpoint nesta fase, a validação relevante é operacional — os serviços sobem via Docker Compose e a aplicação consegue se conectar a todos eles. Isso é verificado por execução real (`docker compose up` + `./mvnw spring-boot:run`/`contextLoads`), não por métrica de cobertura de linha.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhum log estruturado novo nesta fase (não há request de domínio ainda); atenção para garantir que credenciais de datasource/RabbitMQ/Keycloak nunca apareçam em log, mesmo em modo debug.
- **Segurança:** superfície nova = portas expostas do RabbitMQ (`5672`/`15672`) e do Keycloak; nenhuma delas deve ficar acessível com credenciais default/óbvias, mesmo em ambiente de desenvolvimento — toda credencial via variável de ambiente, nunca versionada. `security-auditor` deve revisar isso explicitamente antes do fechamento da fase.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando (`contextLoads` validando conexão real via `spring.docker.compose.skip.in-tests=false`)
- [x] Cobertura ≥ 80% no(s) módulo(s) afetado(s), com sentido — N/A nesta fase (sem lógica de negócio); ver justificativa na seção 7
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D002–D006 + addendum em D004)
- [x] `code-reviewer` executado — achados endereçados: `.gitkeep`/README no diretório de migration, gap issuer OAuth2 documentado como adiado, healthcheck/porta do RabbitMQ, tag de imagem pinada
- [x] `security-auditor` executado — achados endereçados: role dedicada do Keycloak, senhas obrigatórias sem default óbvio, porta do Keycloak em loopback
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando (confirmado localmente, exit code 0)
