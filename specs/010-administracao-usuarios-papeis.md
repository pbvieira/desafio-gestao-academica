# 010 — Administração de usuários e papéis

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §03 (frontend Angular), §04–05 (segurança/observabilidade não
funcionais) — funcionalidade além do escopo literal do PRD, pedida como diferencial pelo Pablo
**Módulo(s) Modulith afetado(s):** novo módulo `administracao` (backend); `security` não é tocado além do
realm export do Keycloak; `frontend/features/administracao/` (novo)

## 1. Objetivo

Dar a um ADMIN a capacidade de listar usuários do Keycloak e reatribuir o papel (ALUNO/SECRETARIA/ADMIN)
de um usuário existente, direto pela UI — sem precisar abrir o console admin do Keycloak. Esta é a
primeira regra real que diferencia ADMIN de SECRETARIA no backend (D011 já registrava essa equivalência
como algo a revisitar quando surgisse uma regra real).

## 2. Escopo

**Dentro do escopo:**
- Endpoint `GET /api/admin/usuarios` — lista usuários do realm `gestao` (username, nome, e-mail, papel
  atual), só ADMIN.
- Endpoint `PATCH /api/admin/usuarios/{id}/papel` — reatribui o papel (ALUNO/SECRETARIA/ADMIN) de um
  usuário existente, só ADMIN.
- Integração com a Admin API do Keycloak via `org.keycloak:keycloak-admin-client` (SDK oficial), usando o
  client confidential `gestao-backend` (D013) com grant `client_credentials`.
- Habilitar `serviceAccountsEnabled: true` no client `gestao-backend` e atribuir ao seu service account os
  client roles `view-users`, `manage-users`, `query-users` de `realm-management`, no
  `docker/keycloak/import/gestao-realm.json` — sem isso a Admin API do Keycloak nunca autentica essa
  chamada.
- Tela `frontend/src/app/features/administracao/usuarios-lista.component.ts`: tabela de usuários com
  papel atual editável por `<select>`, mudança de papel dispara o `PATCH` imediatamente (sem tela de
  formulário separada, dado o escopo pequeno — só um campo mutável).
- Rota `/administracao/usuarios`, guardada por `roleGuard` com `data: { papeis: ['ADMIN'] }`, e item
  correspondente na seção "Administração" da sidebar (slot já criado na spec 009).

**Fora do escopo (não-objetivos):**
- Criar novo usuário, resetar senha, habilitar/desabilitar (`enabled=false`) usuário — qualquer coisa além
  de reatribuir o papel de um usuário já existente (decisão explícita do Pablo, D045).
- Diferenciar mais nenhuma outra permissão entre ADMIN e SECRETARIA além do acesso a esta tela — o resto
  do sistema continua tratando os dois papéis de forma idêntica.
- Auditoria/histórico de quem trocou o papel de quem (poderia ser um evento de domínio publicado para o
  módulo `notificacao`, mas não foi pedido — registrar como possível evolução futura, não como pendência
  desta tarefa).

## 3. Regras de negócio envolvidas

Não há regra do PRD §02 envolvida (isso é uma funcionalidade administrativa além do PRD). Regras novas,
introduzidas por esta spec:

- [x] Só um usuário autenticado com papel ADMIN pode listar usuários ou reatribuir papel — qualquer outro
  papel recebe 403.
- [x] O papel enviado no `PATCH` precisa ser exatamente um de `ALUNO`/`SECRETARIA`/`ADMIN` — qualquer outro
  valor é 400.
- [x] Reatribuir papel a um `id` de usuário que não existe no Keycloak retorna 404.
- [x] Um ADMIN pode reatribuir o próprio papel (não há proteção especial contra "auto-rebaixamento" nesta
  fase — risco aceito, ver seção 8).

## 4. Abordagem técnica

**Novo módulo Modulith `administracao`** (`br.com.desafio.tecnico.gestao.administracao`):
controller → service → cliente Keycloak, camada convencional já usada nos demais módulos.
- `AdministracaoUsuarioController`: `@PreAuthorize("hasRole('ADMIN')")` na classe (mesmo padrão de
  `AlunoController`/D041 para o override pontual, mas aqui a classe inteira já é ADMIN-only, sem
  excecão).
- `AdministracaoUsuarioService`: injeta um `Keycloak` (cliente admin, `KeycloakBuilder.builder()...grantType(CLIENT_CREDENTIALS)`, configurado como `@Bean` em `MensageriaConfig`-style `KeycloakAdminConfig` novo, lendo `issuer-uri`/`client-id`/`client-secret` de `application.properties`/`.env`). Métodos: `listarUsuarios()` (mapeia `UserRepresentation` + role mapping para um DTO simples `UsuarioAdminDto(id, username, nome, email, papel)`), `reatribuirPapel(id, papel)` (remove o role mapping atual entre ALUNO/SECRETARIA/ADMIN e adiciona o novo — um usuário só tem um desses três papéis por vez, por construção do realm).
- DTOs na borda (`UsuarioAdminDto`, `ReatribuirPapelRequest{papel}`), nunca expõe `UserRepresentation` do
  Keycloak diretamente pela API.
- Erros: reusa `RecursoNaoEncontradoException`→404 (já existente, sem exceção nova — ajustado na
  implementação da Task 3 em relação ao rascunho original desta seção); papel inválido no `PATCH`→400 via
  enum Java + `HttpMessageNotReadableException` já tratado pelo `GlobalExceptionHandler`, mesmo padrão de
  `ProblemDetail`/`errorCode` já estabelecido (D016).

**Config Keycloak:** `docker/keycloak/import/gestao-realm.json`, client `gestao-backend`:
`"serviceAccountsEnabled": true`; novo bloco `"serviceAccountClientRoles": {"realm-management":
["view-users", "manage-users", "query-users"]}` (sintaxe do realm export do Keycloak para atribuir client
roles ao service account no import).

**Frontend:** `features/administracao/usuario-admin.service.ts` (GET lista, PATCH papel — mesmo padrão de
`HttpClient`/tratamento de erro dos demais services), `usuarios-lista.component.ts` (tabela reaproveitando
`.data-table` da spec 009, `<select>` inline por linha, `erroGeral`/`carregando` no mesmo padrão dos
demais componentes de listagem).

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Escopo da tela (o que ela gerencia de fato) | Só reatribuir papel vs. + habilitar/desabilitar vs. CRUD completo de usuário | [D045](../docs/DECISIONS.md#d045) — só reatribuir papel |
| 2 | Como o backend chama a Admin API do Keycloak | `keycloak-admin-client` (SDK oficial) vs. REST manual vs. frontend chamando direto (descartada) | [D045](../docs/DECISIONS.md#d045) — `keycloak-admin-client` |
| 3 | Quem acessa a tela | Só ADMIN vs. SECRETARIA e ADMIN (mantém equivalência atual) | [D045](../docs/DECISIONS.md#d045) — só ADMIN |
| 4 | Onde vive o código novo no backend | Módulo novo `administracao` vs. dentro de `security` | [D046](../docs/DECISIONS.md#d046) — módulo novo `administracao` |
| 5 | Como `listarUsuarios()` resolve o papel de cada usuário | N+1 chamadas (1 por usuário) vs. O(3) via membros de cada role gerenciada | [D048](../docs/DECISIONS.md#d048) — O(3), membros de role |

## 6. Critérios de aceite

- [x] ADMIN autenticado vê a lista de usuários do realm `gestao` com papel atual de cada um.
- [x] ADMIN reatribui o papel de um usuário e a mudança é refletida no Keycloak (confirmável via console
  admin do Keycloak ou via novo login desse usuário obtendo um token com o novo papel).
- [x] SECRETARIA/ALUNO recebem 403 ao tentar acessar `GET/PATCH /api/admin/usuarios*` (backend) e não veem
  a rota/seção "Administração" na sidebar (frontend).
- [x] Papel inválido no `PATCH` retorna 400 com `ProblemDetail`; `id` inexistente retorna 404.
- [x] Erros de rede/API tratados na tela (mesmo padrão `erroGeral`/`carregando` dos demais componentes).

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | `AdministracaoUsuarioService` com o cliente Keycloak mockado (listar, reatribuir, papel inválido, usuário inexistente) | `src/test/java/.../administracao/AdministracaoUsuarioServiceTest.java` |
| Integração | `AdministracaoUsuarioController` com Spring Security real (403 para não-ADMIN) — cliente Keycloak ainda mockado/stub, não é um teste de integração real contra Keycloak (ver nota abaixo) | `src/test/java/.../administracao/AdministracaoUsuarioControllerIntegrationTest.java` |
| Unitário (frontend) | `usuario-admin.service.spec.ts` — chamadas HTTP corretas, tratamento de 400/403/404 | `frontend/src/app/features/administracao/usuario-admin.service.spec.ts` |
| E2E | Fluxo completo: ADMIN reatribui papel de um usuário de teste, usuário faz novo login e o token reflete o novo papel | `e2e/administracao-papel-flow.sh` (novo) |

**Justificativa de cobertura:** o risco real desta feature é de autorização (só ADMIN) e de integridade do
role mapping no Keycloak (nunca deixar um usuário com dois papéis simultâneos, ou sem nenhum) — por isso o
teste unitário do service cobre explicitamente a troca (remover o antigo, adicionar o novo) e não só o
caminho feliz. Testar contra um Keycloak real (Testcontainers) foi considerado e descartado nesta fase:
o e2e já validado em `e2e/` roda contra a stack real via Docker Compose, o que cobre esse caminho sem o
custo de manter um segundo mecanismo de integração (Testcontainers-Keycloak) só para este módulo.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** `AdministracaoUsuarioService.reatribuirPapel()` loga em nível INFO, a cada
  reatribuição, o usuário-alvo (id), o papel anterior e o novo papel. "Quem" (o ADMIN autenticado que fez a
  chamada) não é logado explicitamente aqui — o service não tem acesso ao `Authentication` da requisição —
  mas fica implícito no log de acesso HTTP já existente da requisição `PATCH`, correlacionável pelo
  `traceId`/correlation ID já presente em todo log estruturado da aplicação (ver seção "Observabilidade" do
  README). Não há trilha de auditoria persistida nesta fase (fora de escopo, seção 2); este log estruturado
  dá a rastreabilidade mínima prometida, ajustada ao que o código efetivamente cobre (achado da revisão
  final de branch, 2026-07-13).
- **Segurança:** endpoint sensível por natureza (concede/revoga privilégio) — `@PreAuthorize` ADMIN-only
  na classe inteira, sem excecão. **Risco aceito, não mitigado nesta fase:** um ADMIN pode reatribuir o
  próprio papel (ex: se rebaixar acidentalmente, ou outro ADMIN rebaixar todos os demais ADMINs) — sem
  proteção contra "zero ADMIN restante no realm" nesta versão; mitigação possível futura: impedir que o
  último ADMIN do realm rebaixe o próprio papel. O secret do client `gestao-backend`
  (`KEYCLOAK_BACKEND_CLIENT_SECRET`) passa a ter um consumidor de código real (antes não tinha, D013) —
  reforça a necessidade de nunca reusar o valor de exemplo do `.env.example` fora do ambiente local.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando
- [x] Cobertura ≥ 80% no módulo `administracao`, com sentido (ver CLAUDE.md item 5)
- [x] `docs/DECISIONS.md` atualizado com D045 e D046
- [x] `code-reviewer` executado — achados endereçados ou justificadamente descartados
- [x] `security-auditor` executado — achados endereçados ou justificadamente descartados (atenção
  especial ao risco de auto-rebaixamento descrito na seção 8)
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando
- [x] README atualizado — nova seção descrevendo a tela de administração e o requisito de
  `serviceAccountsEnabled` no `gestao-backend`
