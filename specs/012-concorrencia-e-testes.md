# 012 — Concorrência e testes consolidados (Fase 6 unificada)

**Status:** `aprovada`
**Seção(ões) do PRD relacionadas:** §02 (regra de limite de vagas), §06 (critérios eliminatórios — prova de
concorrência é o item mais citado), §07 (diferenciais — observabilidade), §09 (perguntas de entrevista sobre
concorrência e escala)
**Módulo(s) Modulith afetado(s):** `academico` (testes/observabilidade), nenhuma mudança de comportamento em
produção esperada (exceto a métrica da Task 6)

## 1. Objetivo

Provar, com evidência real (não teórica), que a proteção de vaga contra matrícula concorrente
(`UPDATE` condicional atômico + `@Version`, D024/D025, já em produção desde a spec 006) se comporta
corretamente sob disputa real de 20 alunos pela última vaga de uma turma, comparar numericamente essa
estratégia com uma alternativa de lock pessimista, e consolidar as lacunas reais de cobertura/observabilidade
que restam antes do fechamento do desafio. Não adiciona funcionalidade nova de negócio.

## 2. Escopo

**Dentro do escopo:**
- Prova e2e real (HTTP, Keycloak real, não simulado) da disputa de 20 alunos por 1 vaga, com exatamente 1
  confirmação bem-sucedida e 19 rejeitadas com `VAGAS_ESGOTADAS`, repetida 10x consecutivas para provar
  ausência de flakiness. Ferramenta: Playwright `APIRequestContext`, novo projeto `e2e/playwright/`.
- Comparação numérica (corretude, tempo total, contagem de exceções) entre a estratégia atômica já em
  produção e uma variante de lock pessimista, em escala menor (N=10 threads/M=1 vaga), a nível de
  JVM/repository — não HTTP.
- Registro da decisão final (manter atômica vs. trocar) em `docs/DECISIONS.md`, com os números reais das
  duas provas acima.
- Auditoria de cobertura reduzida via relatório JaCoCo (`target/site/jacoco/index.html`), cobrindo só
  lacunas reais identificadas (regras do PRD §02 sem teste, se houver; caminhos de erro não testados nos
  módulos tocados nesta sessão, em particular `administracao`).
- Métrica Micrometer nova (`matricula.vaga.conflito`, com tag `motivo`) para conflitos de vaga na
  confirmação de matrícula, visível num novo painel do dashboard Grafana já existente.
- Seção de "resposta de entrevista preparada" na spec, com os números reais coletados, conectando com D002
  (RabbitMQ) para a pergunta "como isso mudaria com muitas instituições simultâneas".
- Atualização de `docs/ROADMAP.md` (Fase 6 → Concluída) e `README.md` (seção de concorrência + como rodar
  `e2e/playwright/`, hoje não documentado em lugar nenhum).

**Fora do escopo (não-objetivos):**
- Qualquer funcionalidade de negócio nova.
- Particionamento/sharding de dados — fica só como resposta teórica na seção de entrevista.
- Testes de carga com Gatling/JMeter.
- Reserva de vaga com Redis/TTL — já descartada teoricamente por D024 na escala real deste sistema; fica só
  como resposta teórica.
- Revisão exaustiva linha a linha de todos os testes já escritos nas fases anteriores — só lacunas reais.
- Migrar `MatriculaService.confirmar()` para lock pessimista, mesmo que a comparação da Task 3 favoreça essa
  estratégia (cenário tratado como follow-up separado, não implementado nesta fase — ver D051).
- Endpoint de hard-delete para Turma/Curso/Disciplina — não será adicionado só para viabilizar teardown de
  teste (ver D050).

## 3. Regras de negócio envolvidas

Nenhuma regra nova. Esta fase **prova** regras já implementadas:

- [x] "Uma turma possui limite de vagas; confirmar uma matrícula consome uma vaga" (PRD §02) — já
  implementado (D024/D025), esta fase adiciona prova e2e em escala de 20 alunos concorrentes.
- [x] "Um aluno não pode se matricular duas vezes na mesma turma" — já coberto por testes existentes, não
  revisitado aqui.
- [x] Consistência do limite de vagas sob matrícula concorrente (destacado no PRD como ponto crítico de
  avaliação) — objeto central desta fase.

## 4. Abordagem técnica

**Prova e2e (Playwright, `e2e/playwright/`):** projeto Node isolado (D049), `@playwright/test` como única
devDependency, sem instalação de browser (`APIRequestContext` não precisa). Um teste
(`tests/matricula-concorrencia-20-alunos.spec.ts`) que, por repetição (`--repeat-each=10`, `workers: 1`):
1. Garante (idempotente) a existência de 1 curso e 1 disciplina fixos, reaproveitados entre execuções — na
   criação, um 409 é tratado como "já existe", buscando o id via `GET` + filtro (mesmo padrão de
   `e2e/matricula-flow.sh` para o Aluno fixo).
2. Cria 1 turma nova (`limiteVagas=1`, `status=ABERTA`, código com timestamp + índice de repetição) e 20
   Alunos novos, todos via `secretaria.teste` (token real via Resource Owner Password grant contra
   `gestao-frontend`, D036).
3. Cria 20 matrículas PENDENTE (uma por Aluno, mesmo token de secretaria).
4. Dispara as 20 confirmações (`POST /api/matriculas/{id}/confirmar`) via `Promise.all` — sem await
   sequencial entre elas.
5. Assevera exatamente 1×200 e 19×409 com `errorCode: VAGAS_ESGOTADAS`, sem exceção não tratada/timeout.
6. `afterEach`: soft-delete da turma e dos 20 alunos via API (best-effort, log-and-continue por chamada);
   curso/disciplina fixos nunca são tocados (D050).

CI: novo step no job `build` de `.github/workflows/ci.yml`, após os 3 scripts bash existentes — `setup-node`
(Node 22) + `npm ci` + `npx playwright test --repeat-each=10` dentro de `e2e/playwright/`, sem etapa de
download de browser.

**Comparação de lock pessimista (JVM, `src/test/java`):** novo pacote
`br.com.desafio.tecnico.gestao.academico.concorrencia`, só em test sources (D051) — um repository de teste
com `@Lock(LockModeType.PESSIMISTIC_WRITE)` sobre `Turma`, e um serviço transacional mínimo que replica só o
ponto de acesso à vaga (ler com lock, checar `vagasOcupadas < limiteVagas` em Java, incrementar, salvar) —
sem duplicar `MatriculaService` inteiro. Um teste de comparação (`ExecutorService`/`CountDownLatch`, mesmo
estilo de `MatriculaConcorrenciaIntegrationTest`), N=10 threads/M=1 vaga, executado uma vez por estratégia
(a estratégia atômica já existente via `TurmaRepository.consumirVaga`, e a pessimista nova), chamando
repository/service diretamente — não HTTP/MockMvc, contraste deliberado com a prova e2e acima. Loga tempo
total do lote, contagem de sucessos e de exceções de conflito por estratégia.

**Métrica (`MatriculaService`):** injeta `MeterRegistry` (novo, o serviço não tem hoje); no ramo onde
`linhasAfetadas == 0` é resolvido em `confirmar()`, incrementa
`meterRegistry.counter("matricula.vaga.conflito", "motivo", errorCode).increment()` — cobre tanto
`VAGAS_ESGOTADAS` quanto `CONFLITO_CONCORRENCIA`, distinguidos pela tag (D052).

**Painel Grafana:** novo painel em `docker/grafana/provisioning/dashboards/jvm-http-dashboard.json` (único
dashboard do projeto, D009 — mantido como pane-of-glass único em vez de um arquivo novo), mostrando a taxa
de `matricula.vaga.conflito` por `motivo`.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link para DECISIONS.md) |
|---|---|---|---|
| 1 | Onde/como estruturar o projeto Playwright e como expressar a repetição 10x | `e2e/playwright/` isolado vs. dentro de `frontend/`; `--repeat-each` nativo vs. loop bash externo | [D049](../docs/DECISIONS.md#d049) — `e2e/playwright/` isolado, `--repeat-each=10` |
| 2 | Mecanismo de teardown do e2e (dado que teardown completo de curso/disciplina é impossível hoje) | reaproveitar curso/disciplina fixos vs. deixar acumular sem limpeza vs. adicionar hard-delete em produção | [D050](../docs/DECISIONS.md#d050) — curso/disciplina fixos reaproveitados; turma+alunos sempre soft-deletados |
| 3 | Onde vive o código de comparação com lock pessimista | só em `src/test/java` vs. repository method em produção + serviço de comparação em teste | [D051](../docs/DECISIONS.md#d051) — tudo em `src/test/java`, nada em produção |
| 4 | Nome/granularidade da métrica Micrometer nova | contador simples sem tags vs. com tag `motivo` | [D052](../docs/DECISIONS.md#d052) — com tag `motivo` |
| 5 | Decisão final: manter estratégia atômica ou trocar para lock pessimista | a decidir com os números reais das Tasks 2/3 | pendente — registrada em entrada própria de `docs/DECISIONS.md` ao final da Task 4 |

## 6. Critérios de aceite

- [ ] `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts` roda 10x consecutivas
  (`--repeat-each=10`) com exatamente 1×200 e 19×409/`VAGAS_ESGOTADAS` em cada uma, sem exceção não
  tratada/timeout/resultado ambíguo.
- [ ] Teardown (`afterEach`) executa mesmo em caso de falha do teste, soft-deletando turma e os 20 alunos de
  cada repetição.
- [ ] Novo step Playwright incluído e verde em `.github/workflows/ci.yml` (job `build`).
- [ ] Teste de comparação JVM (N=10/M=1) passa para as duas estratégias, com corretude (exatamente 1
  sucesso), tempo total e contagem de exceções logados e coletados.
- [ ] Nenhum código de lock pessimista existe em `src/main/java` (verificável por `grep`).
- [ ] Nova entrada em `docs/DECISIONS.md` registra a decisão final (manter/trocar) com os números reais das
  duas provas.
- [ ] `./mvnw clean verify` passa com o gate JaCoCo ≥80% mantido, incluindo os novos testes.
- [ ] Toda cobertura nova adicionada (Task 5) é rastreável a uma lacuna nomeada (regra PRD §02 ou caminho de
  erro específico), não "para bater número".
- [ ] Contador `matricula.vaga.conflito` (tag `motivo`) incrementa corretamente em conflito real, verificado
  manualmente com dado real no painel Grafana.
- [ ] Spec atualizada com a seção "resposta de entrevista preparada" preenchida com números reais.
- [ ] `docs/ROADMAP.md` (Fase 6 → Concluída) e `README.md` (concorrência + como rodar `e2e/playwright/`)
  atualizados.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Incremento do contador `matricula.vaga.conflito` (Task 6) | `src/test/java/.../academico/service/MatriculaServiceTest.java` (ou equivalente) |
| Integração | Comparação atômica vs. pessimista, N=10/M=1, Testcontainers Postgres real | `src/test/java/.../academico/concorrencia/LockPessimistaVsUpdateAtomicoComparativoTest.java` |
| Integração | Lacunas reais encontradas na auditoria de cobertura (Task 5) — regras PRD §02 e caminhos de erro não testados | `src/test/java/...` (arquivos exatos dependem do que o relatório JaCoCo mostrar) |
| E2E | Disputa real de 20 alunos por 1 vaga, 10x consecutivas, HTTP real + Keycloak real | `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts` |

**Justificativa de cobertura:** o valor desta fase não está em cobertura numérica adicional — o mecanismo de
proteção de vaga já tem cobertura de linha desde a spec 006. O que faltava era prova de comportamento correto
em escala realista (20 concorrentes, não 2) e sobre transporte real (HTTP, não MockMvc), que é exatamente o
que a suíte Playwright entrega; e uma decisão de arquitetura (atômica vs. pessimista) apoiada em números
reais, não em preferência teórica, que é o que o teste de comparação entrega. A auditoria de cobertura
(Task 5) é deliberadamente reduzida — cobre só lacunas reais, não uma métrica-alvo.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** novo contador Micrometer `matricula.vaga.conflito` (tag `motivo`), novo painel
  no dashboard Grafana existente. Nenhum log estruturado novo necessário (os conflitos de vaga já geram log
  via `GlobalExceptionHandler` no tratamento do 409).
- **Segurança:** nenhum endpoint novo/alterado em produção, exceto a injeção de `MeterRegistry` em
  `MatriculaService` (sem superfície de ataque nova — métricas não expõem dado sensível, só contagem
  agregada). O novo projeto Playwright usa as mesmas credenciais de teste já existentes (`secretaria.teste`),
  sem introduzir novo segredo. Atenção ao teardown do e2e: garantir que falhas de limpeza (ex: 409 em uma
  chamada de exclusão) não vazem detalhes de erro sensíveis no log do CI além do necessário para diagnóstico.

## 9. Definition of Done desta tarefa

- [ ] Critérios de aceite (seção 6) atendidos
- [ ] Testes da seção 7 implementados e passando
- [ ] Cobertura ≥ 80% no(s) módulo(s) afetado(s), com sentido (ver CLAUDE.md item 5)
- [ ] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D049-D052 já registradas; decisão
  final da Task 4 pendente até os números reais existirem)
- [ ] `code-reviewer` executado — achados endereçados ou justificadamente descartados
- [ ] `security-auditor` executado — achados endereçados ou justificadamente descartados
- [ ] Esta spec atualizada para refletir o que foi de fato implementado
- [ ] `./mvnw clean verify` passando

## 10. Resposta de entrevista preparada

<A preencher ao final da Task 7, com os números reais coletados nas Tasks 2 e 3.>

**"O que acontece se 20 pessoas tentarem confirmar a última vaga ao mesmo tempo?"**

<Números reais da Task 2: quantas execuções (10/10), resultado de cada uma (1×200/19×409), tempo
observado.>

**"Por que UPDATE atômico condicional em vez de lock pessimista?"**

<Números reais da Task 3: tempo total e contagem de exceções de cada estratégia, com referência a D024 e à
decisão final da Task 4.>

**"Como isso mudaria com muitas instituições simultâneas?"**

<Conexão com D002 (RabbitMQ) — mensageria assíncrona já isola o fluxo de notificação/auditoria do caminho
crítico de confirmação; a contenção de vaga em si é por turma (linha), não por instituição, então o
paralelismo entre instituições diferentes não compete pela mesma linha. Detalhar limites conhecidos (D024
já registra o ponto de revisão: se a escala crescer para "flash sale", reconsiderar reserva via
fila/Redis).>
