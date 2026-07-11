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
| 2 | Domínio: Aluno, Curso, Disciplina, Turma — CRUD completo, camadas, DTOs, validação, testes unitários | Pendente |
| 3 | Matrícula — fluxo funcional completo (criar/confirmar/cancelar, status, duplicidade, consulta por aluno/turma) com proteção básica de vaga | Pendente |
| 4 | Mensageria de domínio: publicar `MatriculaCriada`/`Confirmada`/`Cancelada` no RabbitMQ (infraestrutura já de pé desde a Fase 1) + consumidor no módulo secundário + DLQ/retry | Pendente |
| 5 | Frontend Angular mínimo: telas dos fluxos principais, consumo da API, tratamento de erro | Pendente |
| 6 | Testes: completar unit/integration faltantes, e2e dos fluxos principais de negócio, manter cobertura ≥ 80% (gate JaCoCo já ativo desde a Fase 1) | Pendente |
| 7 | Concorrência aprofundada — estratégia final (otimista/pessimista/constraint), teste de disputa pela última vaga, decisão documentada, resposta de entrevista pronta | Pendente |
| 8 | Documentação final: completar o `README.md` (já existe uma versão inicial, criada na Fase 1) com uso de IA, decisões técnicas, como a vaga é protegida, como a concorrência é tratada | Pendente |

**Nota para a entrevista:** a inversão de prioridade (diferenciais antes do obrigatório) é
o tipo de decisão a explicar com transparência, não a esconder — o trade-off e o motivo de
cada escolha técnica de infraestrutura estão documentados em `docs/DECISIONS.md`. O risco
prático dessa inversão é orçamento: menos folga do que o planejado para as Fases 2-3-7, que
são o que efetivamente decide a régua de avaliação do PRD (§06, critérios críticos).
