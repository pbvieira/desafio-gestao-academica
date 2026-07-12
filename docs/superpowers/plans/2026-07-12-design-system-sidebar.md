# Design System & Sidebar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir a identidade visual atual do frontend (navy/laranja) pela direção "Institucional
acadêmico" (navy + dourado + serifada) aprovada em `specs/009-design-system-sidebar.md`, e trocar o
header horizontal por uma sidebar agrupada por seção/papel — sem alterar nenhuma regra de negócio.

**Architecture:** Duas superfícies de mudança, ambas em `frontend/`: (1) `styles.css` — tokens de design +
classes globais (`.badge`, `.data-table`, `.banner`, botões, campos) reutilizadas por todos os
componentes de feature via classe CSS, nunca duplicadas por componente; (2) `app.html`/`app.css`/`app.ts`
— o shell da aplicação (sidebar + área de conteúdo), consumindo os tokens da superfície (1). Nenhum
componente de feature (`turma-lista`, `curso-lista`, etc.) precisa ser tocado — eles já só referenciam as
classes globais (confirmado por grep em `turma-lista.component.ts`).

**Tech Stack:** Angular 20 standalone, CSS puro com custom properties (sem biblioteca de CSS/design
system externa), SVG inline para ícones.

**Agente recomendado para as Tasks 1 e 2 (via subagent-driven-development):** `ui-designer`
(`.claude/agents/ui-designer.md`) — a paleta e a estrutura já estão fechadas na spec, mas o refinamento de
espaçamento/hierarquia dentro delas é o papel desse subagent.

## Global Constraints

- Tokens exatos (de `specs/009-design-system-sidebar.md` §4): `--navy:#16264A`, `--navy-2:#223768`,
  `--gold:#A9812F` (só estado ativo/ênfase — nunca semântica de erro/aviso), `--bg:#FAFAF7`,
  `--surface:#FFFFFF`, `--border:#E3E1D8`, `--ink:#1C2230`, `--muted:#6B6456`, `--ok:#2F7D5A`,
  `--warn:#B4622E`, `--danger:#A6362D`, `--font-serif:"Iowan Old Style","Palatino Linotype",Palatino,
  Georgia,"Times New Roman",serif` (só `h1`–`h3` e marca; tabela continua em `--font-sans`, maiúsculo, não
  itálico).
- Nenhuma mudança de regra de negócio, endpoint, ou comportamento funcional — só apresentação.
- Nenhum papel pode perder acesso visual a algo que via antes (regressão de navegação é o único risco
  real desta tarefa — ver critério de aceite 1 da spec).
- A seção de sidebar "Administração" (spec 010) **não** é criada nesta tarefa — ver nota de decomposição
  abaixo.
- Dark mode e responsividade mobile refinada continuam fora de escopo (spec 008, não revertido aqui).

**Nota de decomposição (achado ao mapear os arquivos, não previsto na spec):** a spec 009 deixava como
"decisão de sequenciamento na execução" se a seção "Administração" apareceria vazia/placeholder nesta
tarefa ou só quando a spec 010 for implementada. Decisão tomada aqui: a seção "Administração" **não** é
renderizada nesta tarefa — evita um item de navegação morto (`routerLink` para uma rota que ainda não
existe). A spec 010 adiciona a seção e o item juntos, como uma unidade. Também descoberto ao mapear
`app.html`: o link "Turmas" hoje é **incondicional** (visível tanto para ALUNO quanto para
SECRETARIA/ADMIN — é o ponto de entrada para matricular-se E a tela de gestão de turmas). O agrupamento
em seções precisa preservar isso exatamente: "Turmas" aparece tanto na seção "Acadêmico" (staff) quanto na
seção "Minha conta" (ALUNO), não só em uma das duas — do contrário, um papel perderia acesso a algo que
tinha antes (violaria o critério de aceite 1 da spec).

---

### Task 1: Novos tokens de design e classes globais compartilhadas

**Files:**
- Modify: `frontend/src/styles.css` (rewrite completo do arquivo)
- Modify: `frontend/src/app/features/aluno/meu-perfil.component.ts:41` (única referência a
  `var(--color-muted)` fora de `styles.css`/`app.css` — confirmado via
  `grep -rln "color-navy\|color-accent\|color-bg\|color-text\|color-border\|color-surface\|color-danger\|color-success\|color-muted" frontend/src`)

**Interfaces:**
- Consumes: nada (é a base).
- Produces: variáveis CSS `--navy`, `--navy-2`, `--gold`, `--bg`, `--surface`, `--border`, `--ink`,
  `--muted`, `--ok`, `--ok-bg`, `--warn`, `--warn-bg`, `--danger`, `--danger-bg`, `--font-sans`,
  `--font-serif`, `--radius`, `--spacing-{xs,sm,md,lg}` — consumidas pela Task 2. Classes `.badge`
  (+ modificadores `-aberta`/`-confirmada`/`-fechada`/`-cancelada`/`-pendente`), `.data-table`, `.banner`
  (+ `-error`/`-success`), `.loading`, `.field`/`.field-error`, `.toolbar`, `button.primary`/`.danger` —
  consumidas por todos os componentes de feature existentes (nenhum precisa ser modificado).

- [ ] **Step 1: Confirmar que nenhuma outra referência às variáveis antigas escapou do levantamento**

Run: `grep -rln "color-navy\|color-accent\|color-bg\|color-text\|color-border\|color-surface\|color-danger\|color-success\|color-muted" frontend/src`

Expected: exatamente 3 linhas — `frontend/src/styles.css`, `frontend/src/app/app.css`,
`frontend/src/app/features/aluno/meu-perfil.component.ts`. Se aparecer qualquer outro arquivo, pare e
adicione-o ao escopo desta task antes de continuar (renomear uma variável CSS sem atualizar todo
consumidor falha silenciosamente — o valor vira `unset`, sem erro de build).

- [ ] **Step 2: Reescrever `frontend/src/styles.css` por completo**

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
  --ok: #2f7d5a;
  --ok-bg: #e7f1eb;
  --warn: #b4622e;
  --warn-bg: #f7e9df;
  --danger: #a6362d;
  --danger-bg: #f5e4e1;

  --font-sans: -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  --font-serif: 'Iowan Old Style', 'Palatino Linotype', Palatino, Georgia, 'Times New Roman', serif;
  --radius: 3px;
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
}

* {
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  margin: 0;
}

body {
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--ink);
  background: var(--bg);
}

h1,
h2,
h3 {
  font-family: var(--font-serif);
  color: var(--navy);
  margin: 0 0 var(--spacing-md);
}

a {
  color: var(--navy-2);
}

button {
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink);
  padding: 6px 12px;
}

button:hover {
  border-color: var(--navy-2);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

button.primary {
  background: var(--navy);
  border-color: var(--navy);
  color: #fff;
  font-weight: 600;
}

button.primary:hover {
  background: var(--navy-2);
  border-color: var(--navy-2);
}

button.danger {
  color: var(--danger);
  border-color: var(--danger);
}

button.danger:hover {
  background: var(--danger-bg);
}

input,
select {
  font-family: inherit;
  font-size: 14px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  width: 100%;
}

input:focus,
select:focus {
  outline: 2px solid var(--gold);
  outline-offset: 1px;
}

label {
  display: block;
  font-weight: 600;
  margin-bottom: var(--spacing-xs);
  font-size: 13px;
}

.field {
  margin-bottom: var(--spacing-md);
}

.field-error {
  color: var(--danger);
  font-size: 12px;
  margin-top: var(--spacing-xs);
}

.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius);
  margin-bottom: var(--spacing-md);
}

.banner-error {
  background: var(--danger-bg);
  color: var(--danger);
  border: 1px solid var(--danger);
}

.banner-success {
  background: var(--ok-bg);
  color: var(--ok);
  border: 1px solid var(--ok);
}

/* Pills de status: outline (borda + texto coloridos, fundo transparente) - D044.
   `background: currentColor` no ::before herda a cor do modificador sem repetir o hex. */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid currentColor;
  background: transparent;
}

.badge::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.badge-aberta,
.badge-confirmada {
  color: var(--ok);
}

.badge-fechada,
.badge-cancelada {
  color: var(--danger);
}

.badge-pendente {
  color: var(--gold);
}

/* Tabela densa estilo ERP - cabeçalho deixa de ser navy preenchido (pesado em muitas
   linhas) e passa a claro com borda inferior - D044. Continua uma classe CSS global
   reutilizada por todas as listagens, sem componente Angular genérico (D: identidade
   visual, spec 008). */
.data-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--surface);
  border: 1px solid var(--border);
}

.data-table caption {
  text-align: left;
  font-weight: 600;
  font-family: var(--font-serif);
  color: var(--navy);
  padding: var(--spacing-sm) 0;
}

.data-table th,
.data-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}

.data-table th {
  background: var(--bg);
  color: var(--muted);
  font-weight: 600;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  position: sticky;
  top: 0;
}

.data-table tbody tr:hover {
  background: var(--bg);
}

.data-table td.actions {
  display: flex;
  gap: var(--spacing-xs);
  white-space: nowrap;
}

.data-table .empty {
  text-align: center;
  color: var(--muted);
  padding: var(--spacing-lg);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.loading {
  color: var(--muted);
  padding: var(--spacing-md) 0;
}
```

- [ ] **Step 3: Atualizar a única referência sobrevivente à variável antiga**

Em `frontend/src/app/features/aluno/meu-perfil.component.ts:41`, dentro do array `styles`:

```ts
      .perfil dt {
        font-weight: 600;
        color: var(--color-muted);
      }
```

vira:

```ts
      .perfil dt {
        font-weight: 600;
        color: var(--muted);
      }
```

- [ ] **Step 4: Confirmar que não sobrou nenhuma variável antiga**

Run: `grep -rn "color-navy\|color-accent\|color-bg\|color-text\|color-border\|color-surface\|color-danger\|color-success\|color-muted" frontend/src/styles.css frontend/src/app/features/aluno/meu-perfil.component.ts`

Expected: nenhuma saída (nenhum match) — `frontend/src/app/app.css` ainda vai ter matches até a Task 2,
isso é esperado e não faz parte deste comando.

- [ ] **Step 5: Build**

Run: `cd frontend && npx ng build`

Expected: `Application bundle generation complete` sem erros. Um erro de CSS aqui seria sintático (chave
faltando etc.) — variável CSS não-existente NÃO gera erro de build (é `unset` silencioso), por isso o
Step 1/4 acima (grep) é o gate real dessa regressão, não o build.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/styles.css frontend/src/app/features/aluno/meu-perfil.component.ts
git commit -m "feat: novos tokens de design institucional-acadêmico (D044)"
```

---

### Task 2: Shell da aplicação — sidebar substitui o header horizontal

**Files:**
- Modify: `frontend/src/app/app.html` (rewrite completo)
- Modify: `frontend/src/app/app.css` (rewrite completo)
- Modify: `frontend/src/app/app.ts` (adicionar getter `iniciais`)

**Interfaces:**
- Consumes: tokens/classes da Task 1 (`--navy`, `--navy-2`, `--gold`, `--spacing-*`, `--font-serif`);
  `CurrentUser.temPapel(...papeis)`, `CurrentUser.ehSecretariaOuAdmin`, `CurrentUser.papeis`,
  `CurrentUser.nome`, `CurrentUser.username` (todos já existentes em
  `frontend/src/app/core/auth/current-user.ts`, sem alteração); `ErrorBannerService.mensagem()`/`.clear()`
  (já existente).
- Produces: classes `.app-shell`, `.sidebar`, `.brand`, `.brand-mark`, `.brand-name`, `.nav-section`,
  `.nav-label`, `.nav-item`/`.nav-item.active`, `.nav-user`, `.content` (novo shell — nada consome essas
  fora deste arquivo). Getter `protected get iniciais(): string` na classe `App`.

- [ ] **Step 1: Adicionar o getter `iniciais` em `frontend/src/app/app.ts`**

Conteúdo completo do arquivo (adiciona só o getter `iniciais`, resto inalterado):

```ts
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';
import { CurrentUser } from './core/auth/current-user';
import { ErrorBannerService } from './core/error-handling/error-banner.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly currentUser = inject(CurrentUser);
  protected readonly errorBanner = inject(ErrorBannerService);
  private readonly keycloak = inject(Keycloak);

  protected get iniciais(): string {
    const nome = this.currentUser.nome || this.currentUser.username;
    const partes = nome.trim().split(/\s+/).filter(Boolean);
    if (partes.length === 0) {
      return '?';
    }
    if (partes.length === 1) {
      return partes[0].slice(0, 2).toUpperCase();
    }
    return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
  }

  logout(): void {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
```

- [ ] **Step 2: Reescrever `frontend/src/app/app.html` por completo**

```html
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand">
      <span class="brand-mark" aria-hidden="true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l7 3v6c0 5-3 8-7 9-4-1-7-4-7-9V6l7-3z"/></svg>
      </span>
      <span class="brand-name">Gestão Acadêmica</span>
    </div>

    <nav>
      @if (currentUser.ehSecretariaOuAdmin) {
        <div class="nav-section">
          <div class="nav-label">Acadêmico</div>
          <a class="nav-item" routerLink="/turmas" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="4" rx="1"/><rect x="3" y="10" width="18" height="4" rx="1"/><rect x="3" y="16" width="12" height="4" rx="1"/></svg>
            <span>Turmas</span>
          </a>
          <a class="nav-item" routerLink="/cursos" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6 L4 4 V18 L12 20 L20 18 V4 Z"/><line x1="12" y1="6" x2="12" y2="20"/></svg>
            <span>Cursos</span>
          </a>
          <a class="nav-item" routerLink="/disciplinas" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3h12v18l-6-4-6 4V3z"/></svg>
            <span>Disciplinas</span>
          </a>
          <a class="nav-item" routerLink="/alunos" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="3"/><circle cx="16" cy="8" r="3"/><path d="M2 19c0-3 2.7-5 6-5s6 2 6 5"/><path d="M10 19c0-2.5 2.2-4.5 5-4.5s5 2 5 4.5"/></svg>
            <span>Alunos</span>
          </a>
        </div>
      }
      @if (currentUser.temPapel('ALUNO')) {
        <div class="nav-section">
          <div class="nav-label">Minha conta</div>
          <a class="nav-item" routerLink="/turmas" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="4" rx="1"/><rect x="3" y="10" width="18" height="4" rx="1"/><rect x="3" y="16" width="12" height="4" rx="1"/></svg>
            <span>Turmas</span>
          </a>
          <a class="nav-item" routerLink="/minhas-matriculas" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8 12l3 3 5-6"/></svg>
            <span>Minhas matrículas</span>
          </a>
          <a class="nav-item" routerLink="/meu-perfil" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="3.5"/><path d="M4 20c0-3.5 3.5-6 8-6s8 2.5 8 6"/></svg>
            <span>Meu perfil</span>
          </a>
        </div>
      }
    </nav>

    <div class="nav-user">
      <span class="avatar">{{ iniciais }}</span>
      <span class="who">
        <span class="name">{{ currentUser.nome || currentUser.username }}</span>
        <span class="role">{{ currentUser.papeis.join(' · ') }}</span>
      </span>
      <button (click)="logout()" title="Sair" aria-label="Sair">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 4H5a1 1 0 00-1 1v14a1 1 0 001 1h4"/><path d="M16 17l4-5-4-5"/><line x1="20" y1="12" x2="9" y2="12"/></svg>
      </button>
    </div>
  </aside>

  <div class="content">
    @if (errorBanner.mensagem(); as mensagem) {
      <div class="banner banner-error">
        {{ mensagem }}
        <button (click)="errorBanner.clear()">×</button>
      </div>
    }
    <router-outlet />
  </div>
</div>
```

- [ ] **Step 3: Reescrever `frontend/src/app/app.css` por completo**

```css
.app-shell {
  display: grid;
  grid-template-columns: 236px 1fr;
  min-height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: var(--navy);
  color: #e8eaf2;
  padding: 18px 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 16px;
  border-bottom: 1px solid rgba(169, 129, 47, 0.35);
  margin-bottom: 12px;
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
  flex-shrink: 0;
}

.brand-name {
  font-family: var(--font-serif);
  color: #fff;
  font-weight: 700;
  font-size: 0.98rem;
}

.sidebar nav {
  flex: 1;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 20px;
}

.nav-label {
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
  padding: 0 10px 8px;
  color: #b7bfd6;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  font-size: 0.87rem;
  font-weight: 500;
  margin-bottom: 2px;
  color: #cdd3e4;
  text-decoration: none;
}

.nav-item svg {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.nav-item:hover {
  background: rgba(212, 175, 106, 0.1);
}

.nav-item.active {
  background: rgba(212, 175, 106, 0.16);
  color: #f4e4b8;
  box-shadow: inset 2px 0 0 var(--gold);
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 8px 4px;
  margin-top: 8px;
  border-top: 1px solid rgba(169, 129, 47, 0.25);
  font-size: 0.82rem;
}

.nav-user .avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(212, 175, 106, 0.22);
  color: #f4e4b8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  flex-shrink: 0;
}

.nav-user .who {
  flex: 1;
  min-width: 0;
}

.nav-user .name {
  display: block;
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-user .role {
  display: block;
  font-size: 0.72rem;
  color: #c7a75c;
  font-weight: 600;
}

.nav-user button {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  display: flex;
  color: #b7bfd6;
}

.nav-user button:hover {
  background: rgba(212, 175, 106, 0.12);
}

.nav-user button svg {
  width: 16px;
  height: 16px;
}

.content {
  padding: var(--spacing-lg);
  max-width: 1100px;
  overflow-y: auto;
}

@media (max-width: 760px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    flex-direction: row;
    overflow-x: auto;
  }
}
```

- [ ] **Step 4: Build**

Run: `cd frontend && npx ng build`

Expected: `Application bundle generation complete` sem erros.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/app.html frontend/src/app/app.css frontend/src/app/app.ts
git commit -m "feat: sidebar substitui header horizontal (D044/D046)"
```

---

### Task 3: Verificação manual por papel e gate de build/test

**Files:** nenhum arquivo novo — esta task só verifica o resultado das Tasks 1 e 2.

**Interfaces:**
- Consumes: a aplicação completa (Tasks 1 e 2).
- Produces: nenhum artefato de código — o "produzido" é a confirmação de que não houve regressão de
  navegação (critério de aceite 1 da spec 009), registrada na seção 10 da spec ao final.

- [ ] **Step 1: Testes unitários existentes continuam verdes**

Run: `cd frontend && npx ng test --browsers=ChromeHeadless --watch=false`

Expected: `23/23` (mesmo número de testes de antes desta tarefa — Tasks 1/2 não tocam nenhum
`*.service.spec.ts`) — `TOTAL: 23 SUCCESS`.

- [ ] **Step 2: Subir a stack local**

Run (em terminais/processos separados):
```bash
docker compose up -d
cd frontend && npx ng serve
```
E em outro terminal: `./mvnw spring-boot:run` (raiz do repo).

Expected: frontend em `http://localhost:4200`, backend respondendo em `http://localhost:8080/actuator/health`.

- [ ] **Step 3: Verificar visualmente como ALUNO**

Login em `http://localhost:4200` com `aluno.teste`/`aluno123` (credenciais de teste documentadas no
README, seção "Autenticação (Keycloak)").

Expected: sidebar mostra só a seção "Minha conta" com 3 itens — Turmas, Minhas matrículas, Meu perfil (não
deve aparecer a seção "Acadêmico"). Navegar para cada um dos 3 itens confirma que a tela carrega sem
erro no console do browser.

- [ ] **Step 4: Verificar visualmente como SECRETARIA**

Login com `secretaria.teste`/`secretaria123`.

Expected: sidebar mostra só a seção "Acadêmico" com 4 itens — Turmas, Cursos, Disciplinas, Alunos (não
deve aparecer "Minha conta" nem nenhuma seção "Administração"). Navegar para cada um confirma que a
listagem/tabela renderiza com os novos tokens (cabeçalho claro, pills outline no status de Turma/
Matrícula) sem overflow ou texto sobreposto.

- [ ] **Step 5: Verificar visualmente como ADMIN**

Login com `admin.teste`/`admin123`.

Expected: mesma visão da seção "Acadêmico" que SECRETARIA (D011 — ainda sem diferenciação de permissão
nesta tarefa). Nenhuma seção "Administração" deve aparecer ainda (fica para a spec 010).

- [ ] **Step 6: Gate final**

Run: `cd frontend && npx ng build && npx ng test --browsers=ChromeHeadless --watch=false`

Expected: build sem erros, `23/23` testes passando.

Run (raiz do repo): `./mvnw clean verify`

Expected: `BUILD SUCCESS` — esta tarefa não deveria afetar o backend, roda-se só para confirmar que nada
foi quebrado por engano (ex: se algum arquivo backend tivesse sido tocado por erro).

- [ ] **Step 7: Atualizar a spec e commitar**

Marque os critérios de aceite da seção 6 de `specs/009-design-system-sidebar.md` como concluídos e
preencha uma seção "10. Validação manual" (mesmo padrão da spec 008) com o resultado dos Steps 3–6 acima.

```bash
git add specs/009-design-system-sidebar.md
git commit -m "docs: valida e fecha spec 009 (sistema de design + sidebar)"
```

---

## Self-Review

**1. Cobertura da spec:** objetivo (tokens+sidebar) → Tasks 1–2; critério de aceite 1 (sem regressão de
navegação) → Task 3, Steps 3–5, mais a correção do link "Turmas" documentada na Nota de decomposição;
critério "badge/banner/loading seguem o novo estilo" → Task 1; critério "`ng build`/`ng test` continuam
passando" → Task 3, Step 6; seção "Administração" da sidebar → deliberadamente **não** implementada
nesta tarefa (nota de decomposição), ficará no plano da spec 010.

**2. Placeholders:** nenhum "TBD"/"TODO" — todo CSS/HTML/TS acima é conteúdo final, não esqueleto.

**3. Consistência de nomes:** `iniciais` (getter, `app.ts`) usado exatamente assim em `app.html`
(`{{ iniciais }}`); `currentUser.papeis`/`temPapel`/`ehSecretariaOuAdmin`/`nome`/`username` usados com a
assinatura exata já existente em `current-user.ts` (nenhum nome novo inventado do lado do consumidor).

## Execution Handoff

Plan completo e salvo em `docs/superpowers/plans/2026-07-12-design-system-sidebar.md`. Duas opções de
execução:

**1. Subagent-Driven (recomendado)** — dispatco um subagent fresco por task (Tasks 1 e 2 via
`ui-designer`, Task 3 via execução direta), com revisão entre elas.

**2. Execução inline** — executo as tasks nesta sessão via `executing-plans`, em lote, com checkpoints
para revisão.

Qual abordagem?
