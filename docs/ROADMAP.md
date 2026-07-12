# Roadmap de Desenvolvimento

Este documento existe para uso na entrevista técnica: mostra o planejamento em fases feito
antes de começar a desenvolver, e como a execução real divergiu desse plano — útil para
responder perguntas como "qual decisão você tomaria diferente com mais tempo?" ou "como
você reprioriza sob pressão de prazo?" (PRD §09).

## Plano original

Feito antes de iniciar o desenvolvimento, comprimindo o trabalho para caber em ~2 dias
corridos (o PRD estima 8-16h reais de dedicação; total orçado: ~15-18h de trabalho ativo,
com pouca folga para imprevisto).

| Fase | Conteúdo | Estimativa | Categoria PRD |
|---|---|---|---|
| 1 | Infra mínima: compose com Postgres + RabbitMQ + app; migrations base; tratamento de erro padronizado (ProblemDetail); JaCoCo gate | 1,5-2h | Obrigatório |
| 2 | Domínio: Aluno, Curso, Disciplina, Turma — CRUD completo, camadas, DTOs, validação, testes unitários | 3-4h | Obrigatório |
| 3 | Matrícula — fluxo funcional completo (criar/confirmar/cancelar, status, duplicidade, consulta por aluno/turma) com proteção básica de vaga | 2-3h | Obrigatório + Crítico |
| 4 | Mensageria: publicar `MatriculaCriada`/`Confirmada`/`Cancelada` no RabbitMQ + consumidor no módulo secundário + DLQ/retry nativo | 2h | Obrigatório (sênior) |
| 5 | Frontend Angular mínimo: telas dos fluxos principais, consumo da API, tratamento de erro | 2-3h | Obrigatório |
| 6 | Testes: completar unit/integration faltantes, e2e dos fluxos principais, cobertura 80% | 2h | Obrigatório |
| 7 | Concorrência aprofundada — estratégia final (otimista/pessimista/constraint), teste de disputa pela última vaga, decisão documentada, resposta de entrevista pronta | 1,5-2h | Crítico |
| 8 | Documentação final: README completo, doc arquitetural curto, uso de IA. Keycloak/RBAC-ABAC/Grafana-Jaeger-Loki/CI só se sobrar tempo, senão documentar como trade-off consciente | resto do tempo | Diferenciais |

## Como a execução real divergiu do plano

A "Fase 1" que de fato foi implementada (`specs/001-infraestrutura-base.md` a
`specs/004-fundacoes-transversais.md`, ver `docs/DECISIONS.md` para as decisões técnicas de
cada uma) foi muito além do item 1 do plano original. Ela absorveu o item 1 (infra
mínima, ProblemDetail, JaCoCo) **e antecipou a maior parte dos itens da Fase 8**
(Keycloak com RBAC/ABAC, stack completa de observabilidade — Prometheus/Grafana/Jaeger/Loki,
workflow de CI) — que no plano original eram explicitamente condicionais ("só se sobrar
tempo, senão documentar como trade-off consciente").

Ou seja: o que era pra ser feito só na sobra de tempo entrou primeiro, e o núcleo
obrigatório/crítico do PRD (domínio, matrícula, concorrência) ainda não foi iniciado neste
ponto do desenvolvimento.

## Fases restantes (renumeradas a partir do que já está pronto)

| Fase | Conteúdo | Status |
|---|---|---|
| ~~1~~ | ~~Infra mínima~~ + Keycloak/RBAC-ABAC + observabilidade completa + CI (antecipado da Fase 8 original) | **Concluída** — specs 001-004 |
| 2 | Domínio: Aluno, Curso, Disciplina, Turma — CRUD completo, camadas, DTOs, validação, testes unitários | **Concluída** — specs 005 |
| 3 | Matrícula — fluxo funcional completo (criar/confirmar/cancelar, status, duplicidade, consulta por aluno/turma) com proteção básica de vaga | **Concluída** — spec 006 |
| 4 | Mensageria de domínio: publicar `MatriculaCriada`/`Confirmada`/`Cancelada` no RabbitMQ (infraestrutura já de pé desde a Fase 1) + consumidor no módulo secundário + DLQ/retry | **Concluída** — spec 007 |
| 5 | Frontend Angular mínimo: telas dos fluxos principais, consumo da API, tratamento de erro | **Concluída** — spec 008 |
| 5b *(fora do plano original)* | Redesign visual do frontend (nova identidade "Institucional acadêmico" + sidebar) — diferencial pedido pelo Pablo após ver o resultado da Fase 5, não estava no plano original | **Concluída** — spec 009 |
| 5c *(fora do plano original)* | Administração de usuários/papéis (tela ADMIN-only para reatribuir papel via Keycloak Admin API) — diferencial pedido junto com a 5b | **Aprovada, não implementada** — spec 010 |
| 6 | Testes: completar unit/integration faltantes, e2e dos fluxos principais de negócio, manter cobertura ≥ 80% (gate JaCoCo já ativo desde a Fase 1) | Pendente — spec ainda não criada (será `specs/011-*`, não `specs/009-*` — ver nota abaixo) |
| 7 | Concorrência aprofundada — estratégia final (otimista/pessimista/constraint), teste de disputa pela última vaga, decisão documentada, resposta de entrevista pronta | Pendente — mecanismo já implementado (D024); falta só o documento/resposta de entrevista |
| 8 | Documentação final: completar o `README.md` (já existe uma versão inicial, criada na Fase 1) com uso de IA, decisões técnicas, como a vaga é protegida, como a concorrência é tratada | **Concluída para o estado atual** — ver nota abaixo sobre por que isso vai ficar defasado de novo quando a spec 010 for implementada |

**Nota para a entrevista:** a inversão de prioridade (diferenciais antes do obrigatório) é
o tipo de decisão a explicar com transparência, não a esconder — o trade-off e o motivo de
cada escolha técnica de infraestrutura estão documentados em `docs/DECISIONS.md`. O risco
prático dessa inversão é orçamento: menos folga do que o planejado para as Fases 2-3-7, que
são o que efetivamente decide a régua de avaliação do PRD (§06, critérios críticos).

**Nota de reconciliação (2026-07-12):** os status desta tabela estavam desatualizados em relação ao
código/specs já implementados (Fases 2-5 e 8 concluídas, mas ainda marcadas "Pendente") — corrigido numa
tarefa de consolidação de documentação. A concorrência aprofundada (Fase 7) já tem mecanismo implementado
(D024) desde a spec 006; o que resta dessa fase é a resposta de entrevista/documento arquitetural curto
mencionado no PRD §05, ainda não escrito separadamente do que já existe em `docs/DECISIONS.md`.

**Nota sobre numeração de specs/prompts (2026-07-12):** as Fases 5b/5c (redesign visual + administração
de usuários) não estavam no plano original — foram inseridas depois de ver o resultado da Fase 5, e
consumiram os números `specs/009` e `specs/010`. Isso significa que as Fases 6, 7 (e qualquer prompt/spec
já rascunhado para elas antes desta inserção, ex: um rascunho em `.prompts/` que citava
`specs/009-consolidacao-testes.md`) precisam ser renumeradas para `specs/011`, `specs/012`, etc. — **não**
para `009`/`010`, que já existem e estão fechados. Ao dar continuidade às Fases 6/7/8, confirme o próximo
número livre com `ls specs/` antes de criar o arquivo.

**Achado crítico no rascunho da Fase 7 (2026-07-12), antes de executar:** o rascunho em
`.prompts/prompt.010.txt` descreve o cenário de prova real como "20 alunos tentando se matricular
simultaneamente, 1 sucesso, 19 recebem 409 de vaga" e planeja isolar Direct Access Grant num
client/realm de teste separado para autenticar os 20 alunos. Duas premissas verificadas no código atual
(`MatriculaService.java`, `PerfilPermissionEvaluator.java`) mostram que isso não corresponde à
implementação real:
1. `criar()` (matricular-se) nunca verifica vaga — só `confirmar()` faz o `UPDATE` condicional de D024, e
   `confirmar()` é restrito a SECRETARIA/ADMIN (D027). A disputa pela vaga acontece na confirmação, não na
   matrícula em si — "20 alunos se matriculando" nunca geraria um 409 de vaga.
2. `permitirAluno()` libera `MATRICULAR` para qualquer staff (`isStaff(authentication) -> true`) —
   ou seja, um único usuário SECRETARIA/ADMIN já existente pode criar as 20 matrículas PENDENTES em nome
   de 20 Alunos distintos (staff-only, sem precisar de login desses alunos) e depois disparar as 20
   confirmações concorrentes ele mesmo. Não são necessários 20 usuários Keycloak novos, nem isolar
   Direct Access Grant em um client/realm de teste — esse grant já está habilitado no client de produção
   `gestao-frontend` (D036, achado do security-auditor da spec 008, risco já aceito), reaproveitado pelo
   `secretaria.teste` já existente.
   Também: item 1 do rascunho lista "UPDATE atômico condicional" como uma das *duas estratégias novas* a
   implementar para comparação — mas essa estratégia já é a implementação real em produção desde a spec
   006 (D024/D025, confirmado em `TurmaRepository.java`). Só o lock pessimista é, de fato, uma
   implementação nova para a Fase 7.
   Reflita essas duas correções na spec da Fase 7 antes de implementar o cenário e2e/a comparação de
   estratégias.

**Risco de defasagem da Fase 8 (2026-07-12):** a Fase 8 foi marcada "Concluída" com base no estado do
README após a Fase 5b (spec 009) — mas a spec 010 (Fase 5c), quando implementada, vai adicionar uma tela
nova (administração de usuários/papéis) e uma mudança de configuração do Keycloak
(`serviceAccountsEnabled` no client `gestao-backend`) que o README **ainda não descreve**, porque a
funcionalidade não existe em código ainda. A própria spec 010 já lista "README atualizado" na sua
Definition of Done (seção 9) — ou seja, a Fase 8 vai precisar ser reaberta (ou tratada como "concluída
por fase, não globalmente") no momento em que a spec 010 for implementada. Não é um erro atual, é um
lembrete para não deixar o README ficar defasado de novo quando isso acontecer.
