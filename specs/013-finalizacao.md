# 013 — Finalização (Fase 7 renomeada)

**Status:** `aprovada`
**Seção(ões) do PRD relacionadas:** §04 (documentação arquitetural), §05 (README esperado), §06 (critérios
críticos de avaliação), §07 (diferenciais), §08 (como a entrega será analisada), §09 (entrevista técnica)
**Módulo(s) Modulith afetado(s):** nenhum — só documentação (`docs/`, `README.md`, `specs/010`) e evidências
(`docs/images/`)

## 1. Objetivo

Fechar as lacunas reais de documentação/evidência que faltam para o desafio ser avaliado com segurança:
documento arquitetural curto (PRD §04, hoje inexistente como arquivo próprio), explicação legível da
observabilidade, evidência visual de ponta a ponta, auditoria literal contra o PRD §06/§08, e material de
entrevista consolidado. Não adiciona funcionalidade nova nem revisita regra de negócio já fechada.

## 2. Escopo

**Dentro do escopo:**
- `docs/ARQUITETURA.md`: visão geral, 3-5 decisões mais defensáveis com números reais, seção dedicada
  explicando o outbox implementado via Spring Modulith (Event Publication Registry), riscos conhecidos
  aceitos, caminho de evolução para múltiplos serviços, resumo de tratamento de falhas de mensageria.
- `docs/architecture/modules.puml`: saída do `Documenter` do Spring Modulith, evidência automática da
  modularização declarada (D056).
- `docs/OBSERVABILIDADE.md`: papel/funcionamento/conexão/URL/credenciais/"o que olhar primeiro" para
  Prometheus, Grafana, Loki, Promtail, Jaeger — incluindo o painel `matricula.vaga.conflito` (D052) ainda
  não documentado em lugar nenhum.
- Correção de inconsistências já encontradas: `specs/010` com `Status: aprovada` desatualizado; banner de
  status no topo do README desatualizado; 2 arquivos de plano não rastreados em
  `docs/superpowers/plans/`.
- Checklist literal do PRD §04/§05/§06/§08 com evidência apontada (arquivo/seção/teste), não assumida —
  vira a seção 6 desta spec.
- Gate final: `./mvnw clean verify` + suíte `e2e/` completa (bash + Playwright de concorrência) + frontend
  (`ng build`/`ng test`), numa mesma rodada, ambiente inteiro no ar.
- Sessão de captura de evidências visuais (`docs/images/`), embutidas no README/docs com legenda.
- Revisão da seção "Uso de IA" do README frente ao pedido literal do PRD §01/§05 (quais trechos mais
  críticos).
- "Pitch" de entrevista consolidado: índice pergunta provável (PRD §09) → onde está a resposta pronta.

**Fora do escopo (não-objetivos):**
- Qualquer funcionalidade de negócio nova.
- Qualquer mudança de regra de negócio.
- Retrabalho de decisões técnicas já fechadas — exceto se a checklist da seção 5/6 revelar uma lacuna real
  (não estética), caso em que a lacuna é reportada explicitamente antes de qualquer correção.
- Renderização de diagramas PlantUML para imagem (sem ferramenta disponível no ambiente, D056) — o `.puml`
  fica como texto/evidência complementar.

## 3. Regras de negócio envolvidas

Nenhuma — tarefa de documentação e evidência, sem mudança de comportamento do sistema.

## 4. Abordagem técnica

Ver plano de execução completo em `docs/superpowers/plans/2026-07-13-finalizacao.md` (a ser criado),
executado via skill `subagent-driven-development`, na ordem prescrita (não reordenável): (1)
`docs/ARQUITETURA.md`, (2) `docs/OBSERVABILIDADE.md` + resumo no README, (3) consistência
README/ROADMAP/DECISIONS/specs, (4) checklist do PRD com evidência, (5) gate e2e completo + captura, (6)
evidências visuais no README, (7) revisão do "Uso de IA", (8) pitch de entrevista + fechamento.

Cada novo documento narra e linka pontualmente para `docs/DECISIONS.md` (fonte granular de 53+ decisões) em
vez de reescrever o conteúdo — mesmo padrão já usado no projeto entre README (operacional) e DECISIONS.md
(granular).

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link para DECISIONS.md) |
|---|---|---|---|
| 1 | Como capturar evidências visuais sem ferramenta de screenshot nativa | subagent+Playwright browser real vs. captura manual pelo Pablo vs. híbrido | [D054](../docs/DECISIONS.md#d054) — subagent + Playwright, sessão única e descartável |
| 2 | Onde vive a explicação expandida de observabilidade | `docs/OBSERVABILIDADE.md` dedicado vs. expandir a seção do README | [D055](../docs/DECISIONS.md#d055) — `docs/OBSERVABILIDADE.md` dedicado |
| 3 | Como gerar o diagrama de módulos | `Documenter` do Modulith renderizado vs. `.puml` como texto + Mermaid manual vs. só Mermaid manual | [D056](../docs/DECISIONS.md#d056) — `.puml` via Documenter (texto) + Mermaid manual nos fluxos narrativos |

## 6. Critérios de aceite

Checklist literal do PRD, item por item — cada linha marcada `[x]` só quando houver evidência real
apontada (arquivo/seção/teste), preenchida progressivamente pelas Tasks 2-9 deste plano, auditada
formalmente na Task 5.

### PRD §04 — Requisitos Específicos para Dev Sênior

- [ ] Solução modular ou baseada em mais de um serviço — evidência: `docs/ARQUITETURA.md` §visão geral +
  `ModularidadeTest.java` + `docs/architecture/modules.puml`.
- [ ] Separação clara entre contexto acadêmico e contexto de notificações/auditoria/relatórios —
  evidência: pacotes `academico`/`notificacao`, `docs/ARQUITETURA.md`.
- [ ] Comunicação assíncrona usando RabbitMQ, Kafka ou equivalente — evidência: `MensageriaConfig.java`,
  README §Mensageria, D002.
- [ ] Publicação de eventos de domínio (`MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada`) —
  evidência: os 3 records + `MatriculaService.java`.
- [ ] Consumidor de eventos em outro módulo ou serviço — evidência: `MatriculaNotificacaoListener.java`.
- [ ] Garantia de consistência na regra de vagas — evidência: D024/D025, `TurmaRepository.consumirVaga`,
  `MatriculaConcorrenciaIntegrationTest`.
- [ ] Preocupação explícita com concorrência na matrícula — evidência: spec 012 completa, D053,
  `e2e/playwright/`.
- [ ] Testes unitários e de integração nas regras críticas — evidência: cobertura JaCoCo ≥80%,
  `MatriculaServiceTest`, `MatriculaConcorrenciaIntegrationTest`.
- [ ] Docker Compose subindo aplicação, banco de dados e mensageria — evidência: `compose.yaml`, README
  §Como rodar localmente.
- [ ] Observabilidade mínima: logs estruturados, correlation/trace ID, health checks, métricas básicas —
  evidência: `docs/OBSERVABILIDADE.md`, `/actuator/health`, D034.
- [ ] **Documentação arquitetural curta (decisões, trade-offs, riscos conhecidos, evolução, tratamento de
  falhas de mensageria)** — evidência: `docs/ARQUITETURA.md` (esta spec é a que o entrega).
- [ ] README completo — evidência: `README.md` (ver itens do §05 abaixo, item a item).

### PRD §05 — README Esperado (11 itens)

- [ ] Como rodar o projeto localmente.
- [ ] Como subir a solução com Docker Compose.
- [ ] Como executar os testes automatizados.
- [ ] Quais tecnologias foram usadas.
- [ ] Quais foram as principais decisões técnicas.
- [ ] Como a regra de vagas foi protegida.
- [ ] Como a solução trata concorrência na matrícula.
- [ ] Como os eventos de domínio são publicados e consumidos.
- [ ] Como a solução trata falhas na mensageria.
- [ ] Quais logs, health checks, métricas ou mecanismos de rastreabilidade foram implementados.
- [ ] Quais ferramentas de IA foram utilizadas, em quais partes, o que foi revisado manualmente e quais
  trechos são mais críticos.

(Evidência de cada item: seção correspondente do `README.md` — mapeada linha a linha na Task 5.)

### PRD §06 — Critérios Críticos de Avaliação (eliminatórios — 11 itens, todos precisam de evidência de
que o problema NÃO ocorre)

- [ ] Execução: aplicação roda de forma reproduzível, com instrução clara.
- [ ] Stack: backend em Spring Boot.
- [ ] Persistência: há persistência de dados real (Postgres).
- [ ] Regras de negócio: regra de matrícula implementada, regra de vagas não quebra sob concorrência.
- [ ] Camadas: separação clara de responsabilidades, não concentrado em controllers.
- [ ] Testes: há testes para regras críticas de matrícula.
- [ ] Erros: tratamento padronizado de erros (ProblemDetail).
- [ ] Mensageria: há mensageria real com consumidor em outro módulo/serviço.
- [ ] Arquitetura: separação em módulos não é artificial, tem ganho arquitetural real e defensável.
- [ ] Consistência: há preocupação real com consistência/concorrência (prova, não afirmação).
- [ ] Documentação: há documentação arquitetural e README suficientes para executar e validar.
- [ ] Entrevista: o candidato consegue explicar as decisões, o código e o uso de IA (pitch consolidado,
  Task 9).

### PRD §07 — Diferenciais (informativo, não eliminatório — registrar presença/ausência)

- [ ] Outbox Pattern.
- [ ] Idempotência no consumo de mensagens.
- [ ] Retry e dead letter queue.
- [ ] Tracing distribuído.
- [ ] Autenticação e autorização.
- [ ] CI/CD.
- [ ] ADRs curtos.
- [ ] Estratégia clara para refatoração de legado (caminho de evolução).
- [ ] Paginação e filtros.
- [ ] Boa organização do frontend.
- [ ] Boa explicação sobre uso de IA.

### PRD §08 — Como a Entrega Será Analisada (9 dimensões)

- [ ] Funcionalidade entregue.
- [ ] Organização do backend.
- [ ] Regras de negócio.
- [ ] Testes.
- [ ] Frontend.
- [ ] Documentação.
- [ ] Arquitetura/desacoplamento.
- [ ] Mensageria/eventos.
- [ ] Observabilidade/operação.

### Gate final (não é item do PRD, é verificação operacional desta spec)

- [ ] `./mvnw clean verify` verde.
- [ ] Suíte `e2e/` completa verde (3 scripts bash + Playwright `--repeat-each=10`).
- [ ] `ng build` + `ng test --watch=false` verdes.
- [ ] Evidências visuais capturadas e embutidas com legenda (nenhuma imagem "solta").

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário/Integração | Nenhum novo — esta fase não altera código de produção | N/A |
| E2E | Gate final roda toda a suíte já existente (`e2e/*.sh` + `e2e/playwright/`) numa mesma rodada, sem nada quebrado | `e2e/` |

**Justificativa de cobertura:** esta fase é documentação e evidência, não código — não há regra de negócio
nova para testar. O "teste" real desta fase é o gate e2e completo (Task 6) confirmando que nada regrediu, e
a auditoria de checklist (Task 5) confirmando que cada afirmação documentada tem evidência real por trás.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhuma mudança de código — só documentação melhor do que já existe (inclusive
  cobrindo o painel `matricula.vaga.conflito` hoje não documentado).
- **Segurança:** nenhum endpoint novo/alterado. Atenção ao capturar evidências visuais: não expor
  credenciais reais em prints (usar só as credenciais de teste dev-only já documentadas, nunca segredo de
  produção — não há segredo de produção neste projeto, mas o cuidado vale como prática).

## 9. Definition of Done desta tarefa

- [ ] Critérios de aceite (seção 6) atendidos — checklist do PRD §04-§08 completo com evidência real
- [ ] Gate final (seção 6) passando
- [ ] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D054-D056 já registradas)
- [ ] `code-reviewer` executado — achados endereçados ou justificadamente descartados
- [ ] `security-auditor` executado — achados endereçados ou justificadamente descartados
- [ ] Esta spec atualizada para refletir o que foi de fato implementado
- [ ] `./mvnw clean verify` passando
