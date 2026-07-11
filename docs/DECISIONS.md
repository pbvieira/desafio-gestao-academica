# Log de Decisões Técnicas

Este documento existe para um propósito específico: **na entrevista técnica, mostrar exatamente quais
decisões foram deliberadas por mim (Pablo) e quais foram default/sugestão da IA aceita sem alteração.**
Não é um changelog de código — é um changelog de raciocínio.

Gerenciado pela skill `.claude/skills/decision-log/SKILL.md`. Toda decisão técnica não-trivial (mais de uma
alternativa razoável existia) tomada durante o desenvolvimento entra aqui, na ordem em que foi tomada.

## Como ler este documento

Cada entrada tem uma **Origem**, que é o dado mais importante para a entrevista:

| Origem | Significado |
|---|---|
| 🧑 **Decisão do Pablo** | Eu escolhi ativamente entre alternativas, possivelmente contra a sugestão inicial da IA |
| 🤝 **Sugestão da IA, revisada por mim** | A IA propôs, eu ajustei/critiquei antes de aceitar |
| 🤖 **Default da IA, aceito sem alteração** | A IA decidiu (por ser a prática comum) e eu concordei sem mudar nada |

## Índice

<!-- Atualize este índice a cada nova entrada -->
- [D001 — Monólito modular (Spring Modulith) em vez de microsserviços desde o início](#d001)

---

## Template para novas entradas

```markdown
## DNNN — <Título curto da decisão>

**Data:** AAAA-MM-DD
**Origem:** 🧑 / 🤝 / 🤖
**Spec relacionada:** specs/NNN-nome.md
**Contexto:** <por que essa decisão precisou ser tomada — que requisito ou problema a motivou>
**Alternativas consideradas:**
- Alternativa A — <prós/contras>
- Alternativa B — <prós/contras>
**Decisão:** <o que foi escolhido>
**Justificativa:** <por que essa e não as outras>
**Trade-offs aceitos:** <o que se perde ao escolher isso>
**Riscos conhecidos / o que revisitar se o contexto mudar:** <ex: "se o volume de matrículas simultâneas
crescer muito, revisitar a estratégia de lock">
```

---

<a id="d001"></a>
## D001 — Monólito modular (Spring Modulith) em vez de microsserviços desde o início

**Data:** 2026-07-10
**Origem:** 🤝 Sugestão da IA, revisada por mim
**Spec relacionada:** N/A (decisão de fundação, anterior à primeira spec)
**Contexto:** O PRD pede "solução modular ou baseada em mais de um serviço" e "separação clara entre
contexto acadêmico e contexto de notificações/auditoria/relatórios", mas não exige microsserviços
literais — exige desacoplamento real e demonstrável.

**Alternativas consideradas:**
- **Microsserviços desde o início** (2+ deploys separados, comunicação via broker) — atende ao requisito de
  forma mais "óbvia" para quem lê o README, mas para um desafio de 7 dias corridos introduz custo de
  infraestrutura (service discovery, contratos entre serviços, deploy duplicado) desproporcional ao ganho
  real de desacoplamento nesse escopo.
- **Camadas simples sem separação de módulo** — mais rápido, mas não atende ao requisito explícito do PRD
  de separação de contextos, e é justamente o padrão que a seção "Critérios críticos de avaliação" do PRD
  pune ("separação em serviços ou módulos é artificial e sem ganho arquitetural" — nesse caso nem existiria).
- **Monólito modular com Spring Modulith** — desacoplamento real e verificável em tempo de teste
  (`ApplicationModules.verify()`), publicação de eventos de domínio entre módulos, caminho de evolução claro
  para extrair um módulo como serviço separado no futuro (o PRD pergunta isso explicitamente na entrevista:
  "como você separaria esse módulo em outro serviço?").

**Decisão:** Monólito modular com Spring Modulith, dois módulos top-level (`academico` e um módulo
secundário de notificação/auditoria), comunicação entre eles via publicação de eventos de domínio.

**Justificativa:** Maximiza o desacoplamento demonstrável dentro do prazo de 7 dias, e dá uma resposta
concreta e defensável para a pergunta de entrevista sobre evolução para múltiplos serviços — em vez de
implementar microsserviços de forma apressada e superficial.

**Trade-offs aceitos:** Não há isolamento de deploy/processo entre os módulos — se o avaliador entender que
"mais de um serviço" significa estritamente múltiplos processos, essa decisão precisa ser bem defendida
na entrevista com a justificativa acima.

**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o feedback da triagem indicar que a banca
esperava múltiplos deploys de fato, o próximo passo natural é extrair o módulo secundário como serviço
consumidor via o broker externo (RabbitMQ/Kafka) já exigido pelo PRD — a mensageria real já presente na
solução reduz bastante esse custo de extração futura.
