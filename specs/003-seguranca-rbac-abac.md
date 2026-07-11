# 003 — Segurança: RBAC (Keycloak) + mecanismo de ABAC

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §07 (diferencial: autenticação e autorização), §03/§04 (tratamento padronizado de erros)
**Módulo(s) Modulith afetado(s):** novo módulo `security` (cross-cutting, infraestrutura de autenticação/autorização) — distinto dos futuros módulos de domínio (`academico`, `notificacao`).

## 1. Objetivo

Estabelecer a fundação do modelo de autorização: RBAC completo via papéis do Keycloak (realm como código, sem passo manual na UI) e o **mecanismo** de ABAC (como regras de posse serão expressas, aplicadas e testadas no backend), com um exemplo funcional sobre um recurso que já existe nesta fase (perfil do usuário autenticado) — não sobre entidades de domínio, que ainda não existem.

## 2. Escopo

**Dentro do escopo:**
- Realm Keycloak exportado como código (`docker/keycloak/import/gestao-realm.json`), importado automaticamente no `docker compose up` via `start-dev --import-realm` — zero configuração manual na UI.
- Papéis de realm: `ALUNO`, `SECRETARIA`, `ADMIN` (D011).
- Três usuários de teste no próprio realm export (um por papel), só para uso local/manual — nunca dados reais.
- Client `gestao-frontend` (público, Authorization Code + PKCE, com Direct Access Grants habilitado só para viabilizar obtenção manual de token via usuário/senha durante o desenvolvimento/validação — ver seção 4).
- Client `gestao-backend` (confidential, D013) — registrado, mas sem consumidor de código nesta fase.
- Troca de `spring-boot-starter-oauth2-client` por `spring-boot-starter-oauth2-resource-server` no `pom.xml` (D010).
- `SecurityFilterChain` validando Bearer tokens (assinatura + expiração via JWKS do realm) e mapeando `realm_access.roles` do JWT para `GrantedAuthority` (`ROLE_*`).
- Endpoint de exemplo demonstrando RBAC (`@PreAuthorize("hasRole(...)")`) — não sobre `/actuator/health` (para não acoplar autenticação a health checks usados por orquestração/Prometheus — ver spec 002), mas sobre um controller placeholder dedicado.
- Mecanismo de ABAC via `PermissionEvaluator` customizado (D012), com exemplo funcional: `GET /api/usuarios/{id}/perfil` — usuário só vê o próprio perfil, exceto SECRETARIA/ADMIN (override de staff).
- Tratamento padronizado de 401 (não autenticado) vs 403 (autenticado, sem permissão), sem vazar detalhes internos.
- Testes de integração automatizados (RBAC autorizado/não-autorizado + ABAC posse própria/alheia/override de staff), usando JWTs simulados (`SecurityMockMvcRequestPostProcessors.jwt()` do Spring Security Test) — rápidos e sem depender de um Keycloak real no CI.
- Validação manual complementar contra o Keycloak real (via `docker compose`), obtendo tokens de verdade para os 3 papéis.
- `code-reviewer` e `security-auditor` sobre toda a configuração de segurança.

**Fora do escopo (não-objetivos):**
- Qualquer regra de posse sobre Aluno/Curso/Turma/Matrícula — essas entidades não existem ainda (Fase 2). A seção 4 desta spec documenta, como especificação (não implementação), as regras ABAC previstas para quando existirem.
- Keycloak Authorization Services (UMA/policies) — avaliado e descartado como mecanismo de ABAC (D012); toda a lógica de posse fica no código Spring, não no Keycloak.
- Qualquer entidade, tabela, controller ou service de domínio acadêmico — Fase 2.
- Uso efetivo do client `gestao-backend` (D013) — registrado para o futuro, sem nenhuma chamada de código o consumindo nesta fase.
- Subir a própria aplicação no `compose.yaml` — continua adiado (D003).

## 3. Regras de negócio envolvidas

N/A diretamente — esta fase entrega mecanismo de autorização, não regras do PRD §02. A seção 4 abaixo documenta, como especificação futura, como as regras de posse do domínio real (Fase 2) serão expressas usando o mecanismo desta fase.

## 4. Abordagem técnica

### 4.1 Realm Keycloak como código

- `docker/keycloak/import/gestao-realm.json`, montado em `/opt/keycloak/data/import/` no container; `command` do serviço `keycloak` no `compose.yaml` muda de `start-dev` para `start-dev --import-realm` (recurso nativo do Keycloak, sem plugin extra).
- Realm `gestao`. Papéis de **realm** (não de client) — mais simples, suficiente para um único backend + um único frontend consumindo o mesmo conjunto de papéis (D011).
- Clients:
  - `gestao-frontend`: `publicClient=true`, `standardFlowEnabled=true` (Authorization Code + PKCE, para o frontend Angular real), `directAccessGrantsEnabled=true` — habilitado **apenas** para permitir obter tokens via `curl`/Resource Owner Password Grant durante desenvolvimento e validação manual (documentado no README como uso de dev, não recomendado num client público em produção real).
  - `gestao-backend`: `publicClient=false`, sem `standardFlowEnabled` (não inicia login), sem consumidor de código nesta fase. Secret: **não é mais uma string fixa** (ajuste pós-security-review, ver D013) — usa `${env.KEYCLOAK_BACKEND_CLIENT_SECRET}` (substituição nativa do Keycloak no import), lido do ambiente do container a partir de `.env`.
- 3 usuários de teste no export, um por papel (`aluno.teste`, `secretaria.teste`, `admin.teste`), senha de dev fixa e óbvia **de propósito** (são usuários de um Keycloak local efêmero, não de um ambiente real) — documentados no README como credenciais de teste, nunca reais.

### 4.2 Spring Boot — validação de token e RBAC (como implementado)

- `pom.xml`: remove `spring-boot-starter-oauth2-client`, adiciona `spring-boot-starter-oauth2-resource-server` (D010).
- `application.properties`: `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:${KEYCLOAK_HTTP_PORT:8081}/realms/gestao` — a app roda fora do compose (D003), conecta na porta publicada do Keycloak em loopback (mesmo padrão de D004).
- Pacote `br.com.desafio.tecnico.gestao.security` (módulo Modulith próprio, cross-cutting), com 4 subpacotes:
  - `security.config.SecurityConfig`: **duas** `SecurityFilterChain` (não uma só, como planejado originalmente):
    - `@Order(1)`, casada só em `/actuator/prometheus`, com Basic Auth próprio — **adicionado durante a implementação**, achado de security review (H1): esse endpoint expõe padrões de tráfego/latência da API e não podia continuar público agora que existe autorização real de negócio. Credencial dedicada (`spring.security.user.*`), distinta de qualquer papel de usuário.
    - `@Order(2)`, a cadeia "principal": `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**` públicos, demais rotas autenticadas via JWT (Resource Server). CSRF desabilitado e sessão `STATELESS` — **adicionado durante a implementação** (achado de security review, M2): um resource server de Bearer token não usa cookie de sessão, CSRF não se aplica e só atrapalharia métodos de escrita da Fase 2.
  - Conversor customizado de authorities: extrai `realm_access.roles` do JWT e prefixa com `ROLE_`.
  - `security.support.KeycloakClaims`: utilitário compartilhado para ler `realm_access.roles` do JWT — **adicionado durante a implementação** (achado de code review: a lógica estava duplicada, com implementações levemente diferentes, em `SecurityConfig` e em `PerfilController`).
  - `security.config.MethodSecurityConfig`: expõe um `MethodSecurityExpressionHandler` com o `PermissionEvaluator` customizado registrado (D012).
  - `security.web.ExemploAutorizacaoController`: `GET /api/exemplo/staff` com `@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")`.
  - `security.web.PerfilController` + `security.web.PerfilResponse`: `GET /api/usuarios/{id}/perfil`, `@PreAuthorize("hasPermission(#id, 'PERFIL', 'READ')")` — devolve claims do próprio JWT, sem persistência.
  - `security.authorization.PerfilPermissionEvaluator implements PermissionEvaluator` (D012): permite se o subject do JWT == `id` do path **e** a permissão pedida é `"READ"` (qualquer outra nega por padrão — ajuste feito após achado de code review: o parâmetro `permission` era recebido mas nunca checado), **ou** se o usuário tem papel `SECRETARIA`/`ADMIN` (override de staff).
  - `security.config.SecurityErrorHandlingConfig` (pacote `config`, não `web` como planejado originalmente — achado de code review sobre organização): `AuthenticationEntryPoint` (401) e `AccessDeniedHandler` (403), corpo `ProblemDetail` com `status`, `detail` fixo, `timestamp` e `path` — **nunca** stack trace/detalhe interno. Cada negação também gera uma linha de log estruturado (`log.warn`, sem incluir o token) — **adicionado durante a implementação** (achado de code review: a spec já exigia isso na seção 8, mas a primeira versão do código não logava nada).
- **Bug de implementação corrigido:** o corpo de erro inicialmente saía com acentos corrompidos (`response.getWriter()` usa o encoding default da resposta, nem sempre UTF-8) — corrigido escrevendo direto no `OutputStream` via `ObjectMapper`, que usa UTF-8 por padrão.

### 4.3 Regras ABAC previstas para a Fase 2 (especificação, não implementação)

Documentado aqui para não perder o desenho, a implementar quando Aluno/Curso/Turma/Matrícula existirem:

- **Aluno só pode consultar/cancelar a própria matrícula**: `PerfilPermissionEvaluator` (ou um novo `MatriculaPermissionEvaluator` seguindo o mesmo padrão) resolve o dono real da `Matrícula` via repositório (`matricula.aluno.keycloakSubjectId == authentication.subject`), não mais uma comparação direta com um path variable como nesta fase.
- **Secretaria só pode gerenciar turmas de cursos sob sua responsabilidade** — **se essa granularidade for adotada**; o PRD não exige isso explicitamente. Registrar como opção a decidir via decision-log na spec da Fase 2 que tratar de Turma, não comprometer agora.
- **Admin**: sem restrição de posse, mesmo padrão de override já usado no exemplo de perfil desta fase.
- Mecanismo reaproveitado integralmente (D012): novos `targetType` (`"MATRICULA"`, `"TURMA"`) no mesmo `PermissionEvaluator`, ou evoluir para um `PermissionEvaluator` por tipo se o dispatch central ficar grande demais — decisão a tomar na Fase 2 com dados reais de quantos tipos existem.
- **Pendência registrada por achado de security review (M3):** quando SECRETARIA/ADMIN acessam um recurso alheio via override de staff, hoje isso não gera nenhuma trilha de auditoria (só as negações são logadas, seção 4.2). Para dados reais de aluno/matrícula na Fase 2, considerar logar explicitamente "usuário X (staff) acessou o registro do usuário/aluno Y" — necessário para detectar uso indevido de privilégio, não só bloqueio indevido.

## 5. Decisões que requerem confirmação antes de implementar

Todas as decisões não-triviais identificadas para esta spec já foram levantadas e registradas antes desta implementação começar:

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | `oauth2-client` ou `oauth2-resource-server`? | Manter oauth2-client vs. trocar por oauth2-resource-server | [D010](../docs/DECISIONS.md#d010) — oauth2-resource-server |
| 2 | Conjunto de papéis RBAC? | ALUNO/SECRETARIA/ADMIN vs. ALUNO/STAFF | [D011](../docs/DECISIONS.md#d011) — ALUNO, SECRETARIA, ADMIN |
| 3 | Mecanismo de ABAC? | Bean+SpEL vs. PermissionEvaluator vs. Keycloak Authorization Services | [D012](../docs/DECISIONS.md#d012) — PermissionEvaluator customizado |
| 4 | Backend precisa de client Keycloak próprio já? | Nenhum agora vs. já registrar confidential | [D013](../docs/DECISIONS.md#d013) — já registrar `gestao-backend` |

## 6. Critérios de aceite

- [x] `docker compose up` importa o realm `gestao` automaticamente (sem passo manual na UI), com papéis, clients e usuários de teste presentes.
- [x] `pom.xml` usa `oauth2-resource-server` (não mais `oauth2-client`).
- [x] Endpoint RBAC de exemplo: usuário com papel correto → 200; usuário autenticado sem o papel → 403; sem token → 401 (validado com tokens reais dos 3 papéis).
- [x] Endpoint ABAC de exemplo (`/api/usuarios/{id}/perfil`): dono do recurso → 200; outro usuário não-staff → 403; SECRETARIA/ADMIN → 200 mesmo não sendo o dono (validado com tokens reais).
- [x] Corpo de erro 401/403 padronizado, sem stack trace, nome de exceção interna ou detalhe de schema/tabela (com `timestamp`/`path`, bug de encoding UTF-8 corrigido).
- [x] Testes de integração automatizados cobrindo os casos acima (8 testes de RBAC/ABAC + 5 unitários do `PermissionEvaluator`).
- [x] Validação manual com tokens reais dos 3 papéis (via `docker compose` já de pé) confirma o mesmo comportamento.
- [x] Nenhum segredo de produção real no realm export versionado — senhas de usuário de teste são valores de dev; secret do client `gestao-backend` não é mais string fixa (usa `${env.KEYCLOAK_BACKEND_CLIENT_SECRET}`, achado de security review).
- [x] `code-reviewer` executado — achados endereçados: duplicação de parsing de roles extraída para `KeycloakClaims`, `timestamp` adicionado ao erro, log estruturado de negações adicionado, 2 testes de cobertura adicionados (SECRETARIA no override, 401 no endpoint ABAC), `SecurityErrorHandlingConfig` movida para `security.config`.
- [x] `security-auditor` executado — achados endereçados: `/actuator/prometheus` protegido com Basic Auth dedicado (estava público), CSRF desabilitado + sessão STATELESS, secret do client via variável de ambiente nativa do Keycloak, parâmetro `permission` do `PermissionEvaluator` agora checado de fato. Validação de JWT (assinatura/expiração/emissor) e corpos de erro 401/403 confirmados corretos sem necessidade de ajuste.
- [x] `docs/DECISIONS.md` contém D010–D013 (com refinamento pós-review em D013).
- [x] Esta spec atualizada para refletir o que foi de fato implementado.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | `PerfilPermissionEvaluator` isolado (dono, não-dono, SECRETARIA, ADMIN, `targetType` errado) | `PerfilPermissionEvaluatorTest` |
| Integração | RBAC (autorizado/não-autorizado/sem token) e ABAC (dono/alheio/override SECRETARIA/override ADMIN/sem token) via `MockMvc` + JWT simulado | `AutorizacaoIntegrationTest` |
| E2E | N/A nesta fase — validação ponta a ponta com Keycloak real é manual (seção 6), não automatizada | — |

**Justificativa de cobertura:** os testes de integração desta fase servem de **modelo direto** para os testes de ABAC reais da Fase 2 (mesma técnica de JWT simulado, mesmo padrão de asserção 200/403/401) — o valor não é a cobertura de linha em si, é validar que o mecanismo de autorização (RBAC + ABAC) funciona e documentar o padrão de teste a reaproveitar.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** tentativas de acesso negado (401/403) devem aparecer nos logs estruturados já previstos na spec 002, sem incluir o token/credencial em si.
- **Segurança:**
  - JWT validado via JWKS do próprio realm (assinatura + expiração + emissor) — via `JwtDecoders.fromIssuerLocation`, caminho padrão do Spring Security, sem `JwtDecoder` customizado que pudesse abrir brecha; confirmado pelo security review.
  - Nenhum segredo de produção real versionado — as senhas dos usuários de teste são valores de desenvolvimento; o secret do `gestao-backend` vem de variável de ambiente (`${env.KEYCLOAK_BACKEND_CLIENT_SECRET}`), não mais uma string fixa (achado de security review).
  - `/actuator/env`, `/actuator/beans` etc. continuam não expostos (herdado da spec 002).
  - **Achado de security review, corrigido:** `/actuator/prometheus` (público desde a spec 002) não podia continuar assim agora que existe autorização real de negócio — exporia padrões de tráfego/latência da API sem nenhuma credencial. Protegido com Basic Auth dedicado (credencial de scrape, distinta de papéis de usuário).
  - **Achado de security review, corrigido:** CSRF desabilitado e sessão `STATELESS` — um resource server de Bearer token não usa cookie de sessão; sem isso, rotas de escrita da Fase 2 (POST/PUT/DELETE) quebrariam com 403 do filtro CSRF.
  - Mensagens de erro 401/403 são genéricas e fixas — nunca ecoam a exceção interna do Spring Security nem detalhes de schema/tabela; cada negação também gera uma linha de log estruturado (sem o token).

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando (13 testes novos: 8 de integração RBAC/ABAC + 5 unitários do `PermissionEvaluator`)
- [x] Cobertura ≥ 80% no(s) módulo(s) afetado(s), com sentido — aplica-se ao módulo `security`
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D010–D013, com refinamento em D013)
- [x] `code-reviewer` executado — achados endereçados
- [x] `security-auditor` executado — achados endereçados
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando (15 testes no total do projeto, exit code 0)
