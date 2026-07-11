---
name: decision-log
description: Governa o registro de decisões técnicas em docs/DECISIONS.md neste projeto de gestão de matrículas acadêmicas. USE ESTA SKILL SEMPRE que estiver prestes a escolher entre duas ou mais alternativas técnicas razoáveis — biblioteca, padrão de concorrência, estratégia de mensageria, modelagem de dados, abordagem de teste, estrutura de módulo — mesmo que o usuário não tenha pedido explicitamente para "documentar" nada. Consulte também antes de aceitar silenciosamente um default seu (da IA) sem que o usuário tenha opinado. Não é opcional e não é só para decisões "grandes" — é para qualquer momento em que você percebe que está escolhendo em vez de apenas seguindo um caminho único e óbvio.
---

# Decision Log

Este projeto é, ao mesmo tempo, um desafio técnico e material de preparação para entrevista de emprego.
O usuário (Pablo) precisa conseguir, na entrevista, apontar exatamente quais decisões foram dele e quais
foram aceitas de você (Claude) sem questionamento. Essa distinção é o valor central desta skill — sem ela,
o registro vira só mais documentação genérica de arquitetura, que não serve ao propósito real.

## Quando esta skill se aplica

Pergunte-se, a cada decisão de design ou implementação: **"existe mais de uma alternativa razoável aqui?"**

Se sim, é uma decisão não-trivial e passa pelo processo abaixo. Exemplos do que conta:

- Escolha de biblioteca/framework quando há mais de uma opção comum (ex: RabbitMQ vs. Kafka, Testcontainers vs. H2 embarcado)
- Estratégia de concorrência/consistência (ex: lock otimista vs. pessimista na regra de vagas)
- Modelagem de dados com mais de uma forma razoável de representar uma regra do PRD
- Estrutura de módulos/pacotes quando há mais de uma divisão sensata
- Estratégia de tratamento de erro/retry em mensageria
- Qualquer decisão que, se um entrevistador perguntasse "por que você fez assim e não assado", exigisse
  uma resposta pensada (não "foi o padrão que a IA sugeriu")

**O que NÃO conta** (não precisa passar por aqui — não polua o log):
- Nomenclatura de variáveis, formatação, organização de imports
- Escolhas onde só existe uma forma idiomática de fazer (ex: usar `@RestController` para expor um endpoint REST)
- Correções de bugs sem decisão de design envolvida
- Qualquer coisa já decidida e registrada em uma entrada anterior de `docs/DECISIONS.md` — apenas referencie a entrada existente

## Processo

### 1. Detecte o momento da decisão

Assim que perceber que há mais de um caminho razoável, **pare antes de implementar essa parte específica**.
Não implemente uma alternativa e documente depois como se fosse óbvia — a ordem importa: decisão registrada
(ou pelo menos levantada) antes da implementação.

### 2. Classifique a origem provável

Antes de perguntar ao usuário, tenha clareza sobre o que você (IA) faria por padrão, para poder classificar
corretamente depois:

- Se você tem uma opinião/prática padrão clara → é candidata a 🤖 **default da IA** (se o usuário simplesmente concordar) ou 🤝 **sugestão revisada** (se ele ajustar).
- Se a decisão depende de contexto que só o usuário tem (preferência de carreira, o que ele quer aprender/mostrar na entrevista, restrição de tempo) → não decida sozinho, pergunte.

### 3. Apresente a decisão ao usuário

Formato direto, sem enrolação — algo como:

> Preciso decidir [X]. As alternativas razoáveis são [A] e [B]. Meu default seria [A], porque [motivo curto].
> Alguma preferência, ou sigo com o default?

Se o usuário responder "pode seguir"/"tanto faz"/"o que você achar melhor" → isso ainda é uma decisão dele
(ele optou por delegar), mas classifique como 🤖 **default da IA, aceito sem alteração** — porque é isso que
ele vai precisar lembrar na entrevista: "aqui eu deixei a IA decidir".

Se o usuário der uma opinião, mesmo que concorde com seu default por um motivo próprio → classifique como
🧑 **decisão do Pablo**, e capture o motivo dele na entrada, não o seu.

Se o usuário mudar o que você propôs → 🤝 **sugestão revisada**.

### 4. Registre em `docs/DECISIONS.md`

Use o template já presente no topo de `docs/DECISIONS.md`. Preencha:
- Próximo número sequencial (`D002`, `D003`, ...) — confira a última entrada existente.
- Todos os campos do template — não deixe "Trade-offs aceitos" ou "Riscos conhecidos" em branco; se genuinamente não houver, escreva "Nenhum identificado", nunca omita a seção.
- Atualize o índice no topo do arquivo com o link para a nova entrada.

### 5. Referencie a partir da spec

Se a decisão está associada a uma spec ativa (`specs/NNN-*.md`), preencha a tabela da seção 5 dessa spec
com a pergunta e o link (`docs/DECISIONS.md#dnnn`) para a entrada correspondente.

## Regras de estilo para as entradas

- **Contexto e Justificativa em linguagem de entrevista**: escreva como se estivesse respondendo a um
  entrevistador em voz alta, não como documentação de arquitetura formal. Frases como "optei por X porque Y"
  são melhores que descrições passivas ("X foi escolhido devido a Y").
- **Não infle o log**: uma entrada por decisão real. Não crie entradas separadas para sub-decisões triviais
  de uma decisão maior já registrada — adicione como um parágrafo dentro da entrada existente.
- **Seja honesto na origem**: o valor deste documento depende inteiramente de a classificação 🧑/🤝/🤖 ser
  precisa. Classificar tudo como 🧑 para "parecer melhor" destrói o propósito do documento — o Pablo
  especificamente pediu para evidenciar suas próprias tomadas de decisão em contraste com o que foi
  automático, então errar para honestidade, não para aparência.

## Exemplo de entrada mínima válida

```markdown
## D004 — Idempotência no consumidor de eventos de matrícula

**Data:** 2026-07-14
**Origem:** 🧑 Decisão do Pablo
**Spec relacionada:** specs/006-consumidor-notificacao.md
**Contexto:** O consumidor de MatriculaConfirmada pode receber a mesma mensagem mais de uma vez (redelivery
do broker após falha de ack).
**Alternativas consideradas:**
- Sem controle de idempotência — mais simples, mas duplica notificações em caso de redelivery.
- Tabela de eventos processados (dedupe por ID do evento) — pequeno custo de armazenamento, elimina duplicação.
**Decisão:** Tabela de eventos processados.
**Justificativa:** O PRD lista idempotência como diferencial explícito (§07); o custo de implementação é
baixo e demonstra maturidade na entrevista técnica.
**Trade-offs aceitos:** Mais uma tabela para manter, leve overhead de escrita por evento consumido.
**Riscos conhecidos / o que revisitar se o contexto mudar:** Se o volume de eventos crescer muito, considerar
expirar registros antigos da tabela de dedupe.
```