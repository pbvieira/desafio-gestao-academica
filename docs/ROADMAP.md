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
| 5c *(fora do plano original)* | Administração de usuários/papéis (tela ADMIN-only para reatribuir papel via Keycloak Admin API) — diferencial pedido junto com a 5b | **Concluída** — spec 010 (6 tasks executadas via subagent-driven-development, revisão final de branch aprovada em 2026-07-13; decisões D045/D046/D048) |
| 5d *(fora do plano original)* | Tema Keycloakify para o login do Keycloak (identidade visual "Institucional acadêmico" reaplicada às telas de autenticação, entregue como JAR provider via volume) — diferencial pedido junto com a 5b/5c, não estava no plano original | **Concluída** — spec 011 (5 tasks executadas via subagent-driven-development, revisão final de branch aprovada em 2026-07-13; decisão D047) |
| 6 *(unifica as antigas 6+7 — decisão do Pablo, 2026-07-12)* | Testes consolidados + concorrência aprofundada num único ciclo, com a prova e2e da disputa pela última vaga (20 alunos, via confirmação concorrente) como prioridade 1, não como item de auditoria genérica. Comparação com lock pessimista simplificada (sem benchmark de carga elaborado) — ver `.prompts/prompt.009.txt` (reescrito) e a nota "Fase 6 unificada" abaixo | **Concluída** — spec 012 (6 tasks executadas via subagent-driven-development, cada uma com revisão por task; decisões D049-D053; revisão final de branch pendente) |
| 7 *(renomeada da antiga Fase 8 — decisão do Pablo, 2026-07-12)* | Finalização: documento arquitetural curto (PRD §04, deliverable distinto do README que ainda não existe como arquivo próprio), revisão final de consistência entre README/ROADMAP/DECISIONS/specs, checklist literal do PRD §06/§08, gate e2e completo, "pitch" de entrevista consolidado — ver `.prompts/prompt.010.txt` (reescrito) e a nota "Fase 7 reimaginada" abaixo | Pendente |

**Nota para a entrevista:** a inversão de prioridade (diferenciais antes do obrigatório) é
o tipo de decisão a explicar com transparência, não a esconder — o trade-off e o motivo de
cada escolha técnica de infraestrutura estão documentados em `docs/DECISIONS.md`. O risco
prático dessa inversão é orçamento: menos folga do que o planejado para as Fases 2-3-7, que
são o que efetivamente decide a régua de avaliação do PRD (§06, critérios críticos).

**Nota de reconciliação (2026-07-12):** os status desta tabela estavam desatualizados em relação ao
código/specs já implementados (Fases 2-5 e 8 concluídas, mas ainda marcadas "Pendente") — corrigido numa
tarefa de consolidação de documentação. A concorrência aprofundada já tem mecanismo implementado (D024)
desde a spec 006; o que resta é a resposta de entrevista/documento arquitetural curto mencionado no PRD
§05, ainda não escrito separadamente do que já existe em `docs/DECISIONS.md`.

**Nota sobre numeração de specs/prompts (2026-07-12, atualizada em 2026-07-13):** as Fases 5b/5c/5d
(redesign visual + administração de usuários + tema Keycloakify do login) não estavam no plano original —
foram inseridas depois de ver o resultado da Fase 5, e consumiram os números `specs/009`, `specs/010` e
`specs/011` (esta última originalmente cotada para a Fase 6 unificada, mas usada pela 5d — ver spec
`011-tema-keycloakify-login.md`). Isso significa que a Fase 6 (unificada) e a Fase 7 (renomeada) precisam
ser numeradas a partir de `specs/012` — **não** `011`, que já existe e está fechado. Ao dar continuidade,
confirme o próximo número livre com `ls specs/` antes de criar o arquivo.

**Fase 6 unificada (2026-07-12, decisão do Pablo):** as antigas Fases 6 (testes/cobertura) e 7
(concorrência aprofundada) foram fundidas em uma única Fase 6, simplificada, com a prova e2e de disputa
pela última vaga como prioridade 1 (não mais um item de auditoria genérica entre outros). `.prompts/
prompt.009.txt` foi reescrito para refletir isso — inclui também o achado crítico abaixo, já corrigido no
texto reescrito, preservado aqui só como registro de por que o desenho do teste é o que é:

> O rascunho original (`.prompts/prompt.010.txt`, versão anterior) descrevia o cenário de prova real
> como "20 alunos tentando se matricular simultaneamente, 1 sucesso, 19 recebem 409 de vaga" e planejava
> isolar Direct Access Grant num client/realm de teste separado para autenticar os 20 alunos. Duas
> premissas verificadas no código atual (`MatriculaService.java`, `PerfilPermissionEvaluator.java`)
> mostraram que isso não corresponde à implementação real: (1) `criar()` (matricular-se) nunca verifica
> vaga — só `confirmar()` faz o `UPDATE` condicional de D024, e `confirmar()` é restrito a
> SECRETARIA/ADMIN (D027) — a disputa pela vaga acontece na confirmação, não na matrícula em si; (2)
> `permitirAluno()` libera `MATRICULAR` para qualquer staff (`isStaff(authentication) -> true`) — um único
> `secretaria.teste` já existente pode criar as 20 matrículas PENDENTES em nome de 20 Alunos distintos e
> depois disparar as 20 confirmações concorrentes ele mesmo, sem precisar de 20 usuários Keycloak novos
> nem isolar Direct Access Grant (esse grant já está habilitado no client de produção `gestao-frontend`,
> D036, risco já aceito pelo security-auditor da spec 008). Também: "UPDATE atômico condicional" já é a
> implementação real em produção desde a spec 006 (D024/D025) — só o lock pessimista é, de fato,
> implementação nova para esta fase.

**Fase 7 reimaginada (2026-07-12, decisão do Pablo):** renomeada da antiga Fase 8. Em vez de só
"completar o README", o foco passa a ser fechar as lacunas reais que faltam para a entrega: (1) a
**documentação arquitetural curta** exigida pelo PRD §04 ainda não existe como arquivo próprio (só
`docs/DECISIONS.md`, que é a fonte granular, e o README, que é operacional — falta a síntese curta que o
PRD pede como peça distinta); (2) uma passada final de consistência entre README/ROADMAP/DECISIONS/specs
(mesmo tipo de auditoria feita nesta mesma sessão antes de reestruturar as Fases 6-7-8, mas como
checklist formal de fechamento, não reativa); (3) checklist literal do PRD §06 (critérios eliminatórios)
e §08 (como a entrega é avaliada), item por item, com evidência — não assumida por padrão; (4) gate e2e
completo com tudo que foi construído até aqui (matrícula, mensageria, administração de usuários,
concorrência de 20 alunos); (5) um "pitch" de entrevista consolidado — índice de perguntas prováveis →
onde está a resposta pronta, para não precisar procurar sob pressão. `.prompts/prompt.010.txt` foi
reescrito com esse conteúdo; `.prompts/prompt.011.txt` (rascunho vazio da antiga Fase 8) fica sem uso.

O antigo "risco de defasagem da Fase 8" (README desatualizado até a spec 010/Fase 5c ser implementada)
está sendo resolvido à medida que a Fase 5c avança nesta mesma sessão — a Fase 7 reimaginada inclui a
verificação final de que isso de fato aconteceu, não assume.
