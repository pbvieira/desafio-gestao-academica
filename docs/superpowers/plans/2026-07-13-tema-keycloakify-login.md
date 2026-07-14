# Tema Keycloakify para o login — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar a identidade visual "Institucional acadêmico" (navy/dourado/serifada, já validada no Angular
pela spec 009) à tela de login do Keycloak do realm `gestao`, via um tema Keycloakify novo entregue ao
container como JAR provider montado por volume.

**Architecture:** Novo projeto React+Vite em `keycloak-theme/` (clonado do starter oficial do Keycloakify,
toolchain isolada de `frontend/`). Reskin via CSS/tokens + um `Template.tsx` local que substitui só o bloco
de marca do cabeçalho — sem ejetar páginas individuais. Build gera `keycloak-theme/dist_keycloak/keycloak-theme.jar`;
`compose.yaml` monta esse diretório em `/opt/keycloak/providers` no serviço `keycloak`; o realm `gestao`
referencia o tema via `loginTheme` no JSON de import.

**Tech Stack:** Keycloakify 11.x (React 18 + Vite 5), Node 22 / npm (repo já usa npm em `frontend/`, não
yarn), Docker Compose, Keycloak 26 (quay.io/keycloak/keycloak:26.0, Quarkus).

## Global Constraints

- Tema cobre **somente** o tipo `login` (`accountThemeImplementation: "none"`) — spec 011 §2, D047.
- Nome do tema: `gestao-academico` — spec 011 §4, D047.
- Entrega ao Keycloak via **JAR provider montado por volume** em `/opt/keycloak/providers`, sem imagem
  Docker customizada — spec 011 §4, D047.
- Projeto vive em `keycloak-theme/` na raiz do repositório, não dentro de `frontend/` — D047.
- Reskin via CSS/tokens sobre o `Template` default do Keycloakify — **sem** `eject` de páginas individuais
  (login.tsx, error.tsx, etc. continuam vindo de `keycloakify/login/DefaultPage`) — spec 011 §4, D047.
- Tokens visuais exatos (copiados de `frontend/src/styles.css`, não reinventados): `--navy: #16264a`,
  `--navy-2: #223768`, `--gold: #a9812f`, `--bg: #fafaf7`, `--surface: #ffffff`, `--border: #e3e1d8`,
  `--ink: #1c2230`, `--muted: #6b6456`, `--font-serif: 'Iowan Old Style', 'Palatino Linotype', Palatino,
  Georgia, 'Times New Roman', serif`, `--radius: 3px`.
- Bloco de marca idêntico ao usado em `frontend/src/app/app.html` (ícone de escudo SVG inline + texto
  "Gestão Acadêmica") — mesmo path do ícone, adaptado para atributos JSX (`strokeWidth` em vez de
  `stroke-width`, etc., já que este é código React/TSX, não HTML).
- `docker/keycloak/import/gestao-realm.json` ganha `"loginTheme": "gestao-academico"` logo após
  `"registrationAllowed": false,`.
- `compose.yaml`: novo volume no serviço `keycloak`, mesmo padrão read-only já usado para
  `docker/keycloak/import` (`./keycloak-theme/dist_keycloak:/opt/keycloak/providers:ro`).
- Sem testes unitário/integração/E2E novos — tarefa de apresentação pura (spec 011 §7). Verificação é via
  build determinístico (grep de artefatos gerados) + verificação manual do login real.
- Ao verificar com Docker, use `docker compose down` seguido de `docker compose up -d` (não
  `docker compose restart`) — este `compose.yaml` não declara volume persistente para o Postgres do
  Keycloak, então `down`+`up` já garante um `--import-realm` limpo captando o novo `loginTheme` sem
  workaround adicional.
- Commits em português, no estilo já usado no repo (`feat:`, `docs:`, `chore:` + descrição curta).

---

### Task 1: Scaffold do projeto Keycloakify a partir do starter oficial

**Files:**
- Create: `keycloak-theme/` (clonado de `https://github.com/keycloakify/keycloakify-starter.git`, sem
  `.git` próprio, sem `yarn.lock`)
- Modify: `keycloak-theme/package.json` (`name`, `description`)
- Create: `keycloak-theme/package-lock.json` (gerado por `npm install`)

**Interfaces:**
- Consumes: nenhuma (primeira task)
- Produces: projeto Keycloakify funcional em `keycloak-theme/`, buildável via `npm run build-keycloak-theme`,
  gerando `keycloak-theme/dist_keycloak/` — Tasks 2 e 3 modificam arquivos dentro desta estrutura.

- [ ] **Step 1: Clonar o starter oficial e remover o histórico git próprio**

```bash
git clone --depth 1 https://github.com/keycloakify/keycloakify-starter.git keycloak-theme
rm -rf keycloak-theme/.git
rm -f keycloak-theme/yarn.lock
```

- [ ] **Step 2: Ajustar `package.json` para o nome do projeto**

Edite `keycloak-theme/package.json`: troque a linha `"name": "keycloakify-starter",` por
`"name": "gestao-keycloak-theme",` e a linha `"description": "Starter for Keycloakify 11",` por
`"description": "Tema Keycloakify (login) do sistema de gestão acadêmica — ver specs/011-tema-keycloakify-login.md",`.

- [ ] **Step 3: Instalar dependências com npm (não yarn, para consistência com `frontend/`)**

```bash
cd keycloak-theme && npm install
```

Expected: `keycloak-theme/package-lock.json` é criado; `keycloak-theme/node_modules/` existe. Sem erros de
peer dependency fatais (avisos são aceitáveis).

- [ ] **Step 4: Build de baseline (sem nenhuma customização ainda) para confirmar que o scaffold funciona**

```bash
cd keycloak-theme && npm run build-keycloak-theme
```

Expected: termina com `✓ keycloak theme built in` e cria `keycloak-theme/dist_keycloak/` contendo pelo
menos um arquivo `.jar`. Rode `find keycloak-theme/dist_keycloak -maxdepth 1` e confirme que aparece um
`.jar`.

- [ ] **Step 5: Commit**

```bash
git add keycloak-theme
git commit -m "feat: scaffold do projeto Keycloakify (tema de login) a partir do starter oficial"
```

---

### Task 2: Configurar nome do tema, alvo de versão do Keycloak e desabilitar dark mode

**Files:**
- Modify: `keycloak-theme/vite.config.ts`

**Interfaces:**
- Consumes: projeto scaffolded da Task 1 (`npm run build-keycloak-theme` funcional)
- Produces: build determinístico gerando exatamente `keycloak-theme/dist_keycloak/keycloak-theme.jar`
  contendo `theme/gestao-academico/login/...` — Task 4 (compose.yaml) monta esse diretório assumindo esse
  layout exato.

- [ ] **Step 1: Substituir o conteúdo de `keycloak-theme/vite.config.ts`**

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";

export default defineConfig({
    plugins: [
        react(),
        keycloakify({
            accountThemeImplementation: "none",
            themeName: "gestao-academico",
            extraThemeProperties: ["darkMode=false"],
            keycloakVersionTargets: {
                "22-to-25": false,
                "all-other-versions": "keycloak-theme.jar"
            }
        })
    ]
});
```

Nota: `keycloakify` gera `darkMode=true` por padrão dentro de `theme.properties`; `extraThemeProperties`
só permite *adicionar* linhas, não removê-las. O arquivo final terá `darkMode=true` seguido de
`darkMode=false` — isso é esperado e correto: `java.util.Properties` (usado pelo Keycloak para ler esse
arquivo) mantém o **último** valor de uma chave duplicada, então `false` prevalece em tempo de execução.
Não "corrija" isso tentando remover a primeira ocorrência — ela é gerada pela ferramenta, não editável
diretamente.

- [ ] **Step 2: Rebuildar e verificar o artefato**

```bash
cd keycloak-theme && rm -rf dist_keycloak && npm run build-keycloak-theme
find dist_keycloak -maxdepth 1
```

Expected: exatamente um arquivo, `dist_keycloak/keycloak-theme.jar` (nenhum outro `.jar`, já que
`"22-to-25": false` desabilita a variante que não usamos).

- [ ] **Step 3: Verificar o conteúdo do jar (nome do tema e dark mode)**

```bash
cd keycloak-theme
unzip -l dist_keycloak/keycloak-theme.jar | grep "theme/gestao-academico/login/theme.properties"
unzip -p dist_keycloak/keycloak-theme.jar theme/gestao-academico/login/theme.properties | grep darkMode
```

Expected: a primeira linha lista `theme/gestao-academico/login/theme.properties` dentro do jar; a segunda
mostra `darkMode=true` seguido de `darkMode=false` (nessa ordem — confirma que o override foi aplicado por
último).

- [ ] **Step 4: Commit**

```bash
git add keycloak-theme/vite.config.ts
git commit -m "feat: configura nome do tema (gestao-academico), alvo Keycloak 26 e dark mode desabilitado"
```

---

### Task 3: Identidade visual — CSS de tokens + bloco de marca no cabeçalho

**Files:**
- Create: `keycloak-theme/src/login/main.css`
- Modify: `keycloak-theme/src/login/Template.tsx` (cópia local do `Template` default do Keycloakify, com o
  cabeçalho customizado — o arquivo não existe ainda no scaffold; o scaffold usa
  `keycloakify/login/Template` diretamente)
- Modify: `keycloak-theme/src/login/KcPage.tsx:2-6` (trocar o import de `Template`)

**Interfaces:**
- Consumes: `keycloak-theme/src/login/KcContext.ts` (tipo `KcContext`, já existe no scaffold),
  `keycloak-theme/src/login/i18n.ts` (tipo `I18n`, já existe no scaffold) — ambos usados pela assinatura de
  `Template.tsx`, sem alteração de tipo necessária.
- Produces: `Template` default export em `keycloak-theme/src/login/Template.tsx`, consumido por
  `KcPage.tsx` no lugar do `Template` importado de `keycloakify/login/Template`. Nenhuma outra task depende
  de um símbolo novo aqui — o efeito é só visual.

- [ ] **Step 1: Criar `keycloak-theme/src/login/main.css`**

```css
:root {
    --navy: #16264a;
    --navy-2: #223768;
    --gold: #a9812f;
    --bg: #fafaf7;
    --surface: #ffffff;
    --border: #e3e1d8;
    --ink: #1c2230;
    --muted: #6b6456;
    --danger: #a6362d;
    --danger-bg: #f5e4e1;
    --ok: #2f7d5a;
    --ok-bg: #e7f1eb;
    --warn: #b4622e;
    --warn-bg: #f7e9df;

    --font-sans: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    --font-serif: "Iowan Old Style", "Palatino Linotype", Palatino, Georgia, "Times New Roman", serif;
    --radius: 3px;
}

body.kcBodyClass {
    background: var(--bg);
    font-family: var(--font-sans);
    color: var(--ink);
}

#kc-header.kcHeaderClass {
    background: var(--navy);
    padding: 20px 0;
}

#kc-header-wrapper.kcHeaderWrapperClass {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-family: var(--font-serif);
    font-weight: 700;
    font-size: 1.1rem;
}

.brand-mark {
    width: 28px;
    height: 28px;
    border-radius: 6px;
    background: var(--gold);
    color: var(--navy);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.kcFormCardClass {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: none;
}

#kc-page-title {
    font-family: var(--font-serif);
    color: var(--navy);
}

.kcButtonClass.kcButtonPrimaryClass {
    background: var(--navy);
    border-color: var(--navy);
    color: #fff;
    border-radius: var(--radius);
    font-weight: 600;
}

.kcButtonClass.kcButtonPrimaryClass:hover {
    background: var(--navy-2);
    border-color: var(--navy-2);
}

.kcInputClass {
    border: 1px solid var(--border);
    border-radius: var(--radius);
    color: var(--ink);
}

.kcInputClass:focus {
    border-color: var(--gold);
    outline: none;
}

.kcAlertClass {
    border-radius: var(--radius);
}

a {
    color: var(--navy-2);
}
```

- [ ] **Step 2: Criar `keycloak-theme/src/login/Template.tsx`**

Cópia do `Template` default do pacote `keycloakify` (`node_modules/keycloakify/src/login/Template.tsx` na
versão instalada), com duas mudanças: (1) `import "./main.css";` adicionado aos imports, (2) o conteúdo de
`#kc-header-wrapper` substituído pelo bloco de marca (ícone + "Gestão Acadêmica"), no lugar do texto
`{msg("loginTitleHtml", ...)}` que mostraria o nome cru do realm.

```tsx
import { useEffect } from "react";
import { clsx } from "keycloakify/tools/clsx";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { useSetClassName } from "keycloakify/tools/useSetClassName";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { I18n } from "./i18n";
import type { KcContext } from "./KcContext";
import "./main.css";

export default function Template(props: TemplateProps<KcContext, I18n>) {
    const {
        displayInfo = false,
        displayMessage = true,
        displayRequiredFields = false,
        headerNode,
        socialProvidersNode = null,
        infoNode = null,
        documentTitle,
        bodyClassName,
        kcContext,
        i18n,
        doUseDefaultCss,
        classes,
        children
    } = props;

    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

    const { msg, msgStr, currentLanguage, enabledLanguages } = i18n;

    const { realm, auth, url, message, isAppInitiatedAction } = kcContext;

    useEffect(() => {
        document.title = documentTitle ?? msgStr("loginTitle", realm.displayName || realm.name);
    }, []);

    useSetClassName({
        qualifiedName: "html",
        className: kcClsx("kcHtmlClass")
    });

    useSetClassName({
        qualifiedName: "body",
        className: bodyClassName ?? kcClsx("kcBodyClass")
    });

    const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });

    if (!isReadyToRender) {
        return null;
    }

    return (
        <div className={kcClsx("kcLoginClass")}>
            <div id="kc-header" className={kcClsx("kcHeaderClass")}>
                <div id="kc-header-wrapper" className={kcClsx("kcHeaderWrapperClass")}>
                    <span className="brand-mark" aria-hidden="true">
                        <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M12 3l7 3v6c0 5-3 8-7 9-4-1-7-4-7-9V6l7-3z" />
                        </svg>
                    </span>
                    <span className="brand-name">Gestão Acadêmica</span>
                </div>
            </div>
            <div className={kcClsx("kcFormCardClass")}>
                <header className={kcClsx("kcFormHeaderClass")}>
                    {enabledLanguages.length > 1 && (
                        <div className={kcClsx("kcLocaleMainClass")} id="kc-locale">
                            <div id="kc-locale-wrapper" className={kcClsx("kcLocaleWrapperClass")}>
                                <div id="kc-locale-dropdown" className={clsx("menu-button-links", kcClsx("kcLocaleDropDownClass"))}>
                                    <button
                                        tabIndex={1}
                                        id="kc-current-locale-link"
                                        aria-label={msgStr("languages")}
                                        aria-haspopup="true"
                                        aria-expanded="false"
                                        aria-controls="language-switch1"
                                    >
                                        {currentLanguage.label}
                                    </button>
                                    <ul
                                        role="menu"
                                        tabIndex={-1}
                                        aria-labelledby="kc-current-locale-link"
                                        aria-activedescendant=""
                                        id="language-switch1"
                                        className={kcClsx("kcLocaleListClass")}
                                    >
                                        {enabledLanguages.map(({ languageTag, label, href }, i) => (
                                            <li key={languageTag} className={kcClsx("kcLocaleListItemClass")} role="none">
                                                <a role="menuitem" id={`language-${i + 1}`} className={kcClsx("kcLocaleItemClass")} href={href}>
                                                    {label}
                                                </a>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            </div>
                        </div>
                    )}
                    {(() => {
                        const node = !(auth !== undefined && auth.showUsername && !auth.showResetCredentials) ? (
                            <h1 id="kc-page-title">{headerNode}</h1>
                        ) : (
                            <div id="kc-username" className={kcClsx("kcFormGroupClass")}>
                                <label id="kc-attempted-username">{auth.attemptedUsername}</label>
                                <a id="reset-login" href={url.loginRestartFlowUrl} aria-label={msgStr("restartLoginTooltip")}>
                                    <div className="kc-login-tooltip">
                                        <i className={kcClsx("kcResetFlowIcon")}></i>
                                        <span className="kc-tooltip-text">{msg("restartLoginTooltip")}</span>
                                    </div>
                                </a>
                            </div>
                        );

                        if (displayRequiredFields) {
                            return (
                                <div className={kcClsx("kcContentWrapperClass")}>
                                    <div className={clsx(kcClsx("kcLabelWrapperClass"), "subtitle")}>
                                        <span className="subtitle">
                                            <span className="required">*</span>
                                            {msg("requiredFields")}
                                        </span>
                                    </div>
                                    <div className="col-md-10">{node}</div>
                                </div>
                            );
                        }

                        return node;
                    })()}
                </header>
                <div id="kc-content">
                    <div id="kc-content-wrapper">
                        {displayMessage && message !== undefined && (message.type !== "warning" || !isAppInitiatedAction) && (
                            <div
                                className={clsx(
                                    `alert-${message.type}`,
                                    kcClsx("kcAlertClass"),
                                    `pf-m-${message?.type === "error" ? "danger" : message.type}`
                                )}
                            >
                                <div className="pf-c-alert__icon">
                                    {message.type === "success" && <span className={kcClsx("kcFeedbackSuccessIcon")}></span>}
                                    {message.type === "warning" && <span className={kcClsx("kcFeedbackWarningIcon")}></span>}
                                    {message.type === "error" && <span className={kcClsx("kcFeedbackErrorIcon")}></span>}
                                    {message.type === "info" && <span className={kcClsx("kcFeedbackInfoIcon")}></span>}
                                </div>
                                <span
                                    className={kcClsx("kcAlertTitleClass")}
                                    dangerouslySetInnerHTML={{
                                        __html: kcSanitize(message.summary)
                                    }}
                                />
                            </div>
                        )}
                        {children}
                        {auth !== undefined && auth.showTryAnotherWayLink && (
                            <form id="kc-select-try-another-way-form" action={url.loginAction} method="post">
                                <div className={kcClsx("kcFormGroupClass")}>
                                    <input type="hidden" name="tryAnotherWay" value="on" />
                                    <a
                                        href="#"
                                        id="try-another-way"
                                        onClick={event => {
                                            document.forms["kc-select-try-another-way-form" as never].requestSubmit();
                                            event.preventDefault();
                                            return false;
                                        }}
                                    >
                                        {msg("doTryAnotherWay")}
                                    </a>
                                </div>
                            </form>
                        )}
                        {socialProvidersNode}
                        {displayInfo && (
                            <div id="kc-info" className={kcClsx("kcSignUpClass")}>
                                <div id="kc-info-wrapper" className={kcClsx("kcInfoAreaWrapperClass")}>
                                    {infoNode}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
```

- [ ] **Step 3: Trocar o import de `Template` em `keycloak-theme/src/login/KcPage.tsx`**

O arquivo hoje importa `import Template from "keycloakify/login/Template";`. Troque essa linha por:

```typescript
import Template from "./Template";
```

Nenhuma outra linha de `KcPage.tsx` muda.

- [ ] **Step 4: Rebuildar e verificar que a marca aparece no bundle e o CSS foi incluído**

```bash
cd keycloak-theme && rm -rf dist_keycloak dist && npm run build-keycloak-theme
grep -c "Gestão Acadêmica" dist/assets/KcPage-*.js
grep -c "navy" dist/assets/KcPage-*.css
```

Expected: ambos os comandos retornam `1` (ou mais). O primeiro confirma que o bloco de marca está no bundle
JS da SPA (`dist/`, a saída do `vite build` que o `keycloakify build` empacota no jar em
`theme/gestao-academico/login/resources/dist/assets/`); o segundo confirma que `main.css` (com a variável
`--navy`) foi de fato incluído no CSS gerado. Note que `theme/gestao-academico/login/resources/css/login.css`
dentro do jar é o CSS PatternFly padrão do Keycloakify (não o nosso) — não é onde nossas customizações
aparecem; por isso a verificação usa `dist/assets/KcPage-*`, não esse arquivo.

- [ ] **Step 5: Preview visual local (sem precisar do Keycloak real) — verificação manual**

```bash
cd keycloak-theme && npx keycloakify add-story
```

O comando é interativo: mostra uma lista de páginas (`login.ftl`, `register.ftl`, `error.ftl`, etc.) para
escolher. Selecione `login.ftl` (use as setas + Enter). Isso cria
`keycloak-theme/src/login/pages/Login.stories.tsx` (ou caminho equivalente reportado pelo comando). Depois:

```bash
npm run storybook
```

Abra `http://localhost:6006`, encontre a story gerada e confirme visualmente: cabeçalho navy com ícone
dourado + "Gestão Acadêmica", card branco com borda sutil, botão primário navy, título em fonte serifada.
Feche o Storybook (`Ctrl+C`) depois de confirmar — este passo é validação manual, não precisa ficar
rodando. Se preferir pular o Storybook, os greps do Step 4 já são evidência objetiva suficiente de que a
marca e os tokens foram aplicados.

- [ ] **Step 6: Commit**

```bash
git add keycloak-theme/src/login/main.css keycloak-theme/src/login/Template.tsx keycloak-theme/src/login/KcPage.tsx
git commit -m "feat: aplica identidade visual Institucional acadêmico ao login do Keycloak (tokens + bloco de marca)"
```

---

### Task 4: Integrar o tema ao Keycloak local (compose.yaml + realm) e verificar ponta a ponta

**Files:**
- Modify: `compose.yaml:82-84` (serviço `keycloak`, bloco `volumes`)
- Modify: `docker/keycloak/import/gestao-realm.json:5` (adicionar `loginTheme`)

**Interfaces:**
- Consumes: `keycloak-theme/dist_keycloak/keycloak-theme.jar` (produzido pelas Tasks 1-3; precisa existir
  no disco **antes** de `docker compose up`, já que o volume é um bind mount, não um build step do compose)
- Produces: N/A (última mudança de infraestrutura desta spec — Task 5 é só documentação)

- [ ] **Step 1: Adicionar o volume do tema ao serviço `keycloak` em `compose.yaml`**

Localize o bloco `volumes:` do serviço `keycloak` (hoje só a linha do `import`):

```yaml
    volumes:
      - ./docker/keycloak/import:/opt/keycloak/data/import:ro
```

Substitua por:

```yaml
    volumes:
      - ./docker/keycloak/import:/opt/keycloak/data/import:ro
      - ./keycloak-theme/dist_keycloak:/opt/keycloak/providers:ro
```

- [ ] **Step 2: Adicionar `loginTheme` ao realm**

Em `docker/keycloak/import/gestao-realm.json`, logo após a linha `"registrationAllowed": false,`, adicione:

```json
  "loginTheme": "gestao-academico",
```

(o resultado deve ficar com `"registrationAllowed": false,` seguido de `"loginTheme": "gestao-academico",`
antes do bloco `"roles"`.)

- [ ] **Step 3: Garantir que o tema está buildado antes de subir o compose**

```bash
cd keycloak-theme && npm run build-keycloak-theme
ls dist_keycloak/keycloak-theme.jar
```

Expected: o arquivo existe (se a Task 3 já deixou um build, ainda assim rode de novo para garantir que
reflete o estado atual do código).

- [ ] **Step 4: Subir o ambiente do zero (down + up, não restart) e verificar os logs do Keycloak**

```bash
docker compose down
docker compose up -d postgres keycloak
docker compose logs keycloak 2>&1 | tail -60
```

Expected: sem linhas `ERROR` relacionadas a `provider` ou `theme`; o log mostra o boot completo do Keycloak
("Keycloak ... started in ..."). Se aparecer erro de provider, o problema mais provável é o jar não estar
no caminho esperado — confirme com `docker compose exec keycloak ls /opt/keycloak/providers`.

- [ ] **Step 5: Verificar visualmente e via curl que o tema está ativo**

```bash
curl -s "http://localhost:${KEYCLOAK_HTTP_PORT:-8081}/realms/gestao/protocol/openid-connect/auth?client_id=gestao-frontend&response_type=code&scope=openid&redirect_uri=http://localhost:4200" \
  | grep -o "Gestão Acadêmica"
```

Expected: imprime `Gestão Acadêmica` (confirma que o HTML servido pelo Keycloak real, não só o bundle
local, contém o bloco de marca). Complementarmente, abra essa mesma URL no navegador e confirme visualmente
o cabeçalho navy/dourado.

- [ ] **Step 6: Regressão — confirmar que o login funcional (obtenção de token) continua funcionando**

```bash
curl -s -X POST "http://localhost:${KEYCLOAK_HTTP_PORT:-8081}/realms/gestao/protocol/openid-connect/token" \
  -d "client_id=gestao-frontend" -d "grant_type=password" \
  -d "username=aluno.teste" -d "password=aluno123" | jq -r .access_token
```

Expected: imprime um JWT (string longa começando com `eyJ`), igual ao comportamento documentado no README
antes desta mudança — confirma que o novo tema não quebrou o fluxo de autenticação.

- [ ] **Step 7: Commit**

```bash
git add compose.yaml docker/keycloak/import/gestao-realm.json
git commit -m "feat: conecta o tema gestao-academico ao Keycloak local (volume de provider + loginTheme no realm)"
```

---

### Task 5: Documentação — README e fechamento da spec

**Files:**
- Modify: `README.md` (seção `## Autenticação (Keycloak)`)
- Modify: `specs/011-tema-keycloakify-login.md` (marcar critérios de aceite, status, seção 10 se existir
  validação manual a registrar)

**Interfaces:**
- Consumes: nada de código — só documenta o resultado das Tasks 1-4.
- Produces: N/A (última task da spec)

- [ ] **Step 1: Adicionar uma subseção ao README sobre o tema de login**

Logo após o parágrafo que descreve os clients (`gestao-frontend`/`gestao-backend`) na seção
`## Autenticação (Keycloak)`, adicione:

```markdown
**Tema visual do login:** a tela de login do Keycloak usa um tema próprio (Keycloakify,
`keycloak-theme/`, tema `gestao-academico`) que reaplica a mesma identidade "Institucional acadêmico" do
frontend Angular (specs 009 e 011). Antes de subir o Docker Compose (ou sempre que o código em
`keycloak-theme/src` mudar), gere o artefato do tema:

\`\`\`bash
cd keycloak-theme && npm install && npm run build-keycloak-theme
\`\`\`

Isso cria `keycloak-theme/dist_keycloak/keycloak-theme.jar`, montado automaticamente em
`/opt/keycloak/providers` pelo `compose.yaml` (`docker compose up`). Sem esse build, o Keycloak sobe com o
tema padrão (o volume simplesmente fica vazio). Como este `compose.yaml` não mantém volume persistente
para o Postgres do Keycloak, um `docker compose down && docker compose up` sempre reimporta o realm do
zero — não há passo manual adicional para o `loginTheme` ser aplicado.
```

- [ ] **Step 2: Atualizar `specs/011-tema-keycloakify-login.md`**

Marque todos os itens da seção 6 (Critérios de aceite) como `[x]`. Mude `**Status:**` de `aprovada` para
`concluída`. Adicione ao final do arquivo:

```markdown
## 10. Validação manual realizada

- `docker compose down && docker compose up -d postgres keycloak` seguido de inspeção dos logs — sem erro
  de provider/tema.
- `curl .../realms/gestao/protocol/openid-connect/auth?...` retornando HTML com "Gestão Acadêmica".
- Verificação visual no navegador: cabeçalho navy/dourado, card branco, botão primário navy, título em
  fonte serifada.
- `curl .../protocol/openid-connect/token` (grant de senha, `aluno.teste`) continua retornando um access
  token válido — sem regressão no fluxo de autenticação.
```

- [ ] **Step 3: Commit**

```bash
git add README.md specs/011-tema-keycloakify-login.md
git commit -m "docs: documenta o tema Keycloakify no README e fecha a spec 011"
```
