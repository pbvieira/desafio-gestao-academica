# 009 — Sistema de design e sidebar (redesign visual do frontend)

**Status:** `aprovada`
**Seção(ões) do PRD relacionadas:** §03 (frontend Angular consumindo a API), §07 (organização, fluxo de uso
e tratamento de erros — critério de pontuação explícito do frontend, não estética)
**Módulo(s) Modulith afetado(s):** N/A — só `frontend/`, fora do Modulith Java

## 1. Objetivo

Substituir a identidade visual atual (navy/laranja definida na spec 008, cuja execução ficou pobre — sem
hierarquia visual, sem ícones, sem estados vazio/loading tratados) por uma nova identidade "Institucional
acadêmico" (navy profundo + dourado restrito + serifada em títulos), e reestruturar a navegação de um
header horizontal para uma sidebar agrupada por contexto/papel — sem alterar nenhuma regra de negócio ou
fluxo funcional já validado na spec 008.

## 2. Escopo

**Dentro do escopo:**
- Novos tokens de design em `frontend/src/styles.css`, substituindo o bloco `:root` atual.
- Componentes/classes CSS globais reformuladas (reutilizadas por todas as telas via classe, não
  duplicadas por componente): `.data-table`, `.badge` (migra para pill outline), `button.primary`/
  `.danger`, `.banner`, `.loading`/estado vazio, `.field`/`.field-error`.
- Novo shell de navegação em `app.html`/`app.ts`/`app.css`: sidebar fixa com 3 seções condicionais a
  papel — "Acadêmico" (Turmas/Cursos/Disciplinas/Alunos, SECRETARIA/ADMIN), "Minha conta" (Minhas
  matrículas/Meu perfil, ALUNO), "Administração" (só ADMIN — o item "Usuários e Papéis" em si é entregue
  na spec 010; esta spec só cria o slot/seção na sidebar).
- Conjunto mínimo de ícones lineares SVG inline (Turmas, Cursos, Disciplinas, Alunos, Administração,
  Sair), reutilizados na sidebar.
- Aplicação consistente da nova identidade em todas as telas existentes (Turma, Curso, Disciplina, Aluno,
  Matrícula, Meu Perfil, Acesso Negado) — via herança dos estilos globais, sem reescrever a lógica dos
  componentes.

**Fora do escopo (não-objetivos):**
- Dark mode e responsividade mobile refinada (não-objetivos já registrados na spec 008, seção 2).
- A tela de administração de usuários/papéis em si — isso é a spec 010; esta spec só prevê o espaço para
  ela na sidebar.
- Qualquer mudança de regra de negócio, endpoint ou comportamento funcional das telas existentes.
- Geração de novos ícones/ilustrações fora do conjunto mínimo listado acima (sem `visual-asset-generator`/
  MCP nesta fase).

## 3. Regras de negócio envolvidas

Nenhuma nova. A navegação continua condicionada exatamente aos papéis já implementados (ALUNO/
SECRETARIA/ADMIN, `CurrentUser.temPapel`/`ehSecretariaOuAdmin`) — esta tarefa é puramente de
apresentação.

- [ ] Nenhuma ação/tela hoje visível para um papel deixa de estar visível para o mesmo papel após o
  redesign (regressão de navegação é o único risco funcional real desta tarefa).

## 4. Abordagem técnica

**Tokens (`frontend/src/styles.css`):** substituir `--color-navy`/`--color-accent`/etc. por:
`--navy:#16264A`, `--navy-2:#223768`, `--gold:#A9812F` (accent, só para estado ativo/ênfase — nunca para
semântica de erro/aviso), `--bg:#FAFAF7`, `--surface:#FFFFFF`, `--border:#E3E1D8`, `--ink:#1C2230`,
`--muted:#6B6456`, `--ok:#2F7D5A`/`--ok-bg`, `--warn:#B4622E`/`--warn-bg`, `--danger:#A6362D`/
`--danger-bg` (semânticas deliberadamente distintas do `--gold`, para não sobrecarregar o accent com
significado de erro/aviso), `--font-serif:"Iowan Old Style","Palatino Linotype",Palatino,Georgia,"Times
New Roman",serif` (só `h1`–`h3` e nome da marca — cabeçalho de tabela continua em `--font-sans`, maiúsculo,
não itálico, para legibilidade em alta densidade de dados). `.badge` migra de preenchido para pill outline
(borda colorida, fundo transparente) — mais formal, coerente com a direção "institucional".
`.data-table th` deixa de ser navy-preenchido e passa a cabeçalho claro com borda inferior (o preenchido
atual, a essa nova paleta, ficaria pesado demais em telas com muitas linhas).

**Shell (`app.html`/`app.ts`/`app.css`):** troca do `<header>` + `<nav>` horizontal atual por um grid
`sidebar (fixa, ~236px) | conteúdo`, replicando a estrutura já validada no mockup de aprovação: bloco de
marca no topo, `<nav>` com `nav-section`/`nav-label`/`nav-item` (estado ativo via `routerLinkActive`,
mesmo mecanismo já usado), rodapé da sidebar com avatar/nome/papel do usuário + botão Sair (mesma lógica
de `CurrentUser`/`logout()` já existente em `app.ts`, só reposicionada no template).

**Reuso:** como os componentes de feature já usam só classes globais (`.data-table`, `.badge`, `.banner`,
`.loading`, `button.primary`/`.danger`, `.field`/`.field-error` — confirmado em
`turma-lista.component.ts` e no mesmo padrão nos demais), o impacto visual fica concentrado em
`styles.css` + no shell; os templates de feature não precisam ser tocados.

**`ui-designer` (subagent):** conduz o refinamento fino de espaçamento, hierarquia tipográfica e estados
vazio/loading dentro dos tokens acima — a paleta e a estrutura da sidebar já estão fechadas nesta spec,
não são decisão do subagent.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Direção visual da nova identidade | SaaS/ERP neutro vs. Institucional acadêmico vs. EdTech vibrante (mockup comparativo) | [D044](../docs/DECISIONS.md#d044) — Institucional acadêmico |
| 2 | Agrupamento de navegação da sidebar | 3 seções por contexto/papel vs. lista plana com ícones | [D046](../docs/DECISIONS.md#d046) — 3 seções, com slot antecipado para Administração |

## 6. Critérios de aceite

- [ ] Todas as telas existentes renderizam com os novos tokens (navy/dourado/serifada), sem regressão
  visual quebrada (overflow, contraste ilegível, texto sobreposto).
- [ ] Sidebar substitui o header horizontal; os mesmos itens de navegação de hoje continuam acessíveis,
  agrupados nas 3 seções, cada um condicionado ao mesmo papel de antes.
- [x] ~~Seção "Administração" aparece na sidebar só para ADMIN (mesmo sem nenhum item funcional dentro
  dela ainda...)~~ — **revertido durante a execução (D046, refinamento 2026-07-12):** a seção não é criada
  nesta spec, para não deixar um item de navegação sem rota real por trás; a spec 010 cria a seção e o
  item juntos, como uma unidade.
- [ ] `.badge`/status pill, `.banner`, `.loading`, estado vazio de tabela seguem o novo estilo em pelo
  menos Turma, Aluno e Matrícula (telas com mais uso desses padrões).
- [ ] Nenhum teste unitário de service quebra (esta tarefa não toca lógica, só template/CSS).
- [ ] `ng build` e `ng test` continuam passando.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Nenhum teste novo esperado (mudança de apresentação, não de lógica) — os `*.service.spec.ts` existentes continuam passando sem alteração | `frontend/src/app/features/**/*.service.spec.ts` |
| Manual | Navegação por papel (ALUNO, SECRETARIA, ADMIN) confirmando que a sidebar mostra exatamente as seções/itens esperados, e que nenhuma tela existente regrediu visualmente | Documentado na seção 10 ao fechar (mesmo padrão da spec 008) |
| E2E | Sem novo E2E — `e2e/matricula-flow.sh` (contra a API) não é afetado por uma mudança puramente de frontend | `e2e/matricula-flow.sh` |

**Justificativa de cobertura:** esta é uma tarefa de apresentação, não de lógica de negócio — o risco real
é regressão de navegação (um papel perder acesso a algo que tinha antes), não cobertura de linha. Por
isso o teste mais valioso é a validação manual por papel, não um teste automatizado novo.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhum impacto — nenhuma chamada de API nova ou alterada.
- **Segurança:** nenhum impacto — a visibilidade de itens de navegação já era só UX (a autorização real
  continua inteiramente no backend, RBAC/ABAC inalterados). Nenhum dado sensível novo exposto.

## 9. Definition of Done desta tarefa

- [ ] Critérios de aceite (seção 6) atendidos
- [ ] Testes da seção 7 implementados e passando
- [ ] Cobertura ≥ 80% no módulo afetado — N/A nesta tarefa (sem lógica nova); build/lint verde é o gate
  real
- [ ] `docs/DECISIONS.md` atualizado com D044 e D046
- [ ] `code-reviewer` executado — achados endereçados ou justificadamente descartados
- [ ] `security-auditor` executado — achados endereçados ou justificadamente descartados
- [ ] Esta spec atualizada para refletir o que foi de fato implementado
- [ ] `./mvnw clean verify` passando (não deveria ser afetado por esta tarefa, mas roda-se para confirmar)
- [ ] README atualizado se a estrutura de navegação documentada nele (se houver) tiver mudado
