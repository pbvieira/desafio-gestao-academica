# 011 — Tema Keycloakify para o login do Keycloak

**Status:** `aprovada`
**Seção(ões) do PRD relacionadas:** §05 (documento arquitetural/README deve cobrir Keycloak e como o
usuário interage com ele), critério transversal de "identidade visual coerente" do desafio
**Módulo(s) Modulith afetado(s):** N/A — só `keycloak-theme/` (novo, fora do Modulith Java) e
`docker/keycloak/import/gestao-realm.json`

## 1. Objetivo

Aplicar a identidade visual "Institucional acadêmico" (já validada no Angular pela spec 009) às telas de
login do Keycloak, para que a experiência de autenticação não pareça um produto de terceiros dentro do
sistema.

## 2. Escopo

**Dentro do escopo:**
- Novo projeto Keycloakify em `keycloak-theme/` (React + Vite, toolchain próprio, isolado do
  `frontend/` Angular).
- Tema `gestao-academico`, tipo `login` apenas — cobre a tela de login e páginas de erro do fluxo de
  autenticação (o realm `gestao` não tem `resetPasswordAllowed: true` hoje, então "esqueci minha senha"
  não é uma tela alcançável; se isso mudar no futuro, herda o tema automaticamente por já usar o mesmo
  `Template`, sem trabalho extra).
- Reaproveitamento dos tokens visuais do `frontend/src/styles.css` (`--navy: #16264A`, `--navy-2:
  #223768`, `--gold: #a9812f` só para estado ativo/ênfase, `--bg`, `--surface`, `--border`, `--ink`,
  `--muted`, `--font-serif` para títulos, `--font-sans` para o resto) e do bloco de marca já usado na
  sidebar do Angular (ícone de escudo SVG inline + nome "Gestão Acadêmica").
- Build via `npm run build-keycloak-theme` gerando `keycloak-theme/dist_keycloak/`, alvo Keycloak 26
  (Quarkus).
- `compose.yaml`: novo volume montando `./keycloak-theme/dist_keycloak` em `/opt/keycloak/providers`
  (read-only) no serviço `keycloak`.
- `docker/keycloak/import/gestao-realm.json`: adicionar `"loginTheme": "gestao-academico"`.
- README atualizado com o passo de build do tema antes de `docker compose up` (o volume só existe depois
  desse build) e com nota sobre a URL de login usar o novo tema.

**Fora do escopo (não-objetivos):**
- Account console, tema de e-mail e admin console do Keycloak — a aplicação Angular já resolve
  autoatendimento de conta ("Meu Perfil"), então o usuário final não navega para essas telas do Keycloak
  hoje.
- Reescrita de qualquer página de login em React customizado — o reskin é via CSS/tokens sobre o
  `Template` default do Keycloakify, sem `eject` de páginas individuais.
- Qualquer mudança de fluxo de autenticação, política de senha, ou configuração de realm além do
  `loginTheme`.
- Imagem Docker customizada do Keycloak — a entrega do tema é só via volume (ver D047).

## 3. Regras de negócio envolvidas

Nenhuma — tarefa puramente de apresentação, sem regra do PRD §02 envolvida.

- [x] Nenhum fluxo de autenticação/autorização existente (RBAC/ABAC, client `gestao-backend`,
  `gestao-frontend`) muda de comportamento — só a aparência da tela de login muda.

## 4. Abordagem técnica

**Scaffold:** `npx create-keycloakify@latest` (ou adição manual de `keycloakify` + deps React/Vite) dentro
de `keycloak-theme/`, configurado para gerar só o tipo de tema `login` (não `account`/`email`/`admin`).

**Reskin:** sem `eject` de páginas individuais do Keycloakify (login.tsx, error.tsx, etc. continuam usando
o `Template` default, que já resolve estrutura/acessibilidade/i18n). Duas customizações pontuais:
1. CSS global (`src/login/main.css` ou equivalente) com as mesmas custom properties do
   `frontend/src/styles.css` — copiado do bloco `:root`, não reinventado.
2. Override do componente `Template` (header/logo) para trocar o logo padrão do Keycloak pelo mesmo
   bloco de marca da sidebar Angular (ícone de escudo SVG + "Gestão Acadêmica").

**Build/target:** `keycloakify.config` (`themeName: ["gestao-academico"]`, tipo `login`) configurado para
gerar artefato compatível com Keycloak 26 (Quarkus, JAR provider).

**Integração com o Keycloak local:** `compose.yaml` monta `./keycloak-theme/dist_keycloak` inteiro (não um
arquivo `.jar` específico) em `/opt/keycloak/providers`, read-only — evita acoplar o nome exato do artefato
gerado pelo build. `start-dev` detecta o novo provider e refaz o build interno do Keycloak automaticamente
no próximo start (sem passo manual de `kc.sh build`). O realm `gestao` já referencia o tema via
`loginTheme` no JSON de import, mantendo o padrão de "realm como código" já usado no projeto.

**Verificação visual:** Keycloakify inclui um modo de desenvolvimento local com `kcContext` mockado
(`npm run dev` dentro de `keycloak-theme/`) para iterar no CSS/Template sem precisar subir o Keycloak a
cada mudança. A verificação final ainda é manual, subindo o compose e acessando a tela de login real do
realm `gestao`.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Escopo de páginas do tema | Somente login vs. login+account vs. reskin completo (login+account+email+admin) | [D047](../docs/DECISIONS.md#d047) — somente login |
| 2 | Estratégia de entrega do tema ao container Keycloak | Diretório de tema sem JAR vs. JAR provider via volume vs. imagem Docker customizada | [D047](../docs/DECISIONS.md#d047) — JAR provider via volume |
| 3 | Local do projeto Keycloakify no repositório | `keycloak-theme/` na raiz vs. dentro de `frontend/` | [D047](../docs/DECISIONS.md#d047) — `keycloak-theme/` na raiz |
| 4 | Estratégia de reskin das páginas | CSS/tokens sobre o `Template` default vs. reescrever cada página em React customizado (`eject`) | [D047](../docs/DECISIONS.md#d047) — CSS/tokens sobre o `Template` default |

## 6. Critérios de aceite

- [ ] Tela de login do realm `gestao` (`/realms/gestao/protocol/openid-connect/auth?...`) exibe a paleta
  navy/dourado, fonte serifada no título e o bloco de marca (ícone + "Gestão Acadêmica"), em vez do tema
  padrão do Keycloak.
- [ ] Tela de erro genérica do Keycloak também reflete o novo tema (mesmo `Template`, herdado
  automaticamente).
- [ ] `docker/keycloak/import/gestao-realm.json` referencia `loginTheme: gestao-academico` e o Keycloak
  sobe sem erro de tema não encontrado.
- [ ] `docker compose up` (após o build do tema) não requer nenhum passo manual adicional dentro do
  container para o tema ficar disponível.
- [ ] Nenhum fluxo de login/autenticação existente (RBAC/ABAC, emissão de token, redirecionamento
  pós-login do Angular) regride.
- [ ] README atualizado com o passo de build do tema e a observação sobre a nova aparência do login.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Nenhum — sem lógica de negócio nova | N/A |
| Integração | Nenhum novo — o fluxo de autenticação já é validado pelos testes existentes de segurança (specs 003); esta tarefa não altera esse comportamento | N/A |
| Manual | Validação visual da tela de login/erro do realm `gestao` após `docker compose up`, confirmando os tokens visuais e que o login continua funcional ponta a ponta (login → redirecionamento ao Angular → token válido) | Documentado na seção 10 ao fechar |
| E2E | Sem novo E2E — `e2e/matricula-flow.sh` já cobre o fluxo de autenticação contra a API; a aparência do tema não afeta esse teste | `e2e/matricula-flow.sh` |

**Justificativa de cobertura:** tarefa puramente de apresentação de uma tela que não pertence à aplicação
Java/Angular (é servida pelo próprio Keycloak) — não há regra de negócio para cobrir com teste
automatizado; o risco real é regressão visual ou quebra do fluxo de login, ambos verificáveis manualmente
de forma mais direta que com um teste automatizado dedicado.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhum impacto — nenhuma mudança em endpoint, log estruturado ou métrica.
- **Segurança:** nenhum impacto direto — o tema é só apresentação (CSS/HTML/imagens estáticas); nenhuma
  lógica de autenticação, validação de token ou fluxo OAuth2 é alterada. Atenção apenas para não
  introduzir HTML/JS que reintroduza campos de formulário fora do fluxo controlado pelo `Template` padrão
  do Keycloakify (risco de quebrar CSRF/proteções já embutidas no template base).

## 9. Definition of Done desta tarefa

- [ ] Critérios de aceite (seção 6) atendidos
- [ ] Testes da seção 7 (validação manual) executados e documentados
- [ ] `docs/DECISIONS.md` atualizado com D047
- [ ] `code-reviewer` executado — achados endereçados ou justificadamente descartados
- [ ] `security-auditor` executado — achados endereçados ou justificadamente descartados
- [ ] Esta spec atualizada para refletir o que foi de fato implementado
- [ ] README atualizado (passo de build do tema)
- [ ] `./mvnw clean verify` continua passando (nenhuma mudança em `src/`, mas verificação de não-regressão)
