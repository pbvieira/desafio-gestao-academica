# 012 — Concorrência e testes consolidados (Fase 6 unificada)

**Status:** `concluída`
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
| 5 | Decisão final: manter estratégia atômica ou trocar para lock pessimista | atômica (D024) vs. pessimista (`PESSIMISTIC_WRITE`) — números reais: ambas 1 sucesso/9 conflitos/0 exceções em N=10/M=1, tempos estatisticamente indistinguíveis (32ms vs 33ms) | [D053](../docs/DECISIONS.md#d053) — mantém a estratégia atômica (D024) |

## 6. Critérios de aceite

- [x] `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts` roda 10x consecutivas
  (`--repeat-each=10`) com exatamente 1×200 e 19×409/`VAGAS_ESGOTADAS` em cada uma, sem exceção não
  tratada/timeout/resultado ambíguo.
- [x] Teardown (`afterEach`) executa mesmo em caso de falha do teste, soft-deletando turma e os 20 alunos de
  cada repetição.
- [x] Novo step Playwright incluído e verde em `.github/workflows/ci.yml` (job `build`).
- [x] Teste de comparação JVM (N=10/M=1) passa para as duas estratégias, com corretude (exatamente 1
  sucesso), tempo total e contagem de exceções logados e coletados.
- [x] Nenhum código de lock pessimista existe em `src/main/java` (verificável por `grep`).
- [x] Nova entrada em `docs/DECISIONS.md` registra a decisão final (manter/trocar) com os números reais das
  duas provas.
- [x] `./mvnw clean verify` passa com o gate JaCoCo ≥80% mantido, incluindo os novos testes.
- [x] Toda cobertura nova adicionada (Task 5) é rastreável a uma lacuna nomeada (regra PRD §02 ou caminho de
  erro específico), não "para bater número".
- [x] Contador `matricula.vaga.conflito` (tag `motivo`) incrementa corretamente em conflito real, verificado
  manualmente com dado real no painel Grafana.
- [x] Spec atualizada com a seção "resposta de entrevista preparada" preenchida com números reais.
- [x] `docs/ROADMAP.md` (Fase 6 → Concluída) e `README.md` (concorrência + como rodar `e2e/playwright/`)
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

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando
- [x] Cobertura ≥ 80% no(s) módulo(s) afetado(s), com sentido (ver CLAUDE.md item 5)
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5 (D049-D053 registradas)
- [x] `code-reviewer` executado — revisão por task (uma por task 2-7) + revisão final de branch sobre o
  diff completo (2026-07-13, "Ready to merge: With fixes" — 3 achados triviais de documentação/comentário,
  todos aplicados no mesmo commit da revisão)
- [x] `security-auditor` executado — revisão por task + revisão final de branch sobre o diff completo
  (2026-07-13, "Ready to merge: Yes", sem achados bloqueantes)
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando

## 10. Resposta de entrevista preparada

Três perguntas prováveis (PRD §09) com a resposta pronta, apoiada em números reais coletados nesta fase —
não em raciocínio teórico.

**"O que acontece se 20 pessoas tentarem confirmar a última vaga ao mesmo tempo?"**

Testado de verdade, não simulado: `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts` cria 1
turma com 1 vaga e 20 alunos, e dispara as 20 confirmações via `POST /api/matriculas/{id}/confirmar`
verdadeiramente em paralelo (`Promise.all`, sem `await` sequencial entre elas), por HTTP real contra a
aplicação de pé e autenticação real via Keycloak (não MockMvc, não JWT simulado). Rodado com
`--repeat-each=10`, duas vezes seguidas — **20 execuções no total** — e em **todas as 20** o resultado foi
idêntico: **exatamente 1 requisição recebeu `200` (a confirmação bem-sucedida) e as outras 19 receberam
`409` com `errorCode: VAGAS_ESGOTADAS`**. Zero exceção não tratada, zero timeout, zero resultado ambíguo
(nunca 2 sucessos, nunca 0 sucessos) em nenhuma das execuções. O teardown (soft-delete da turma e dos 20
alunos de cada execução, preservando curso/disciplina fixos — D050) também rodou sem falha nas 20 vezes.
Isso é a prova empírica de que o `UPDATE` condicional atômico (D024, em produção desde a spec 006) se
comporta corretamente sob disputa real, em escala de 20 concorrentes — não só nos 2 threads dos testes de
integração já existentes.

**"Por que UPDATE atômico condicional em vez de lock pessimista?"**

Comparação numérica isolada a nível de JVM/repository (N=10 threads disputando M=1 vaga, uma execução por
estratégia, código de comparação vivendo só em `src/test/java/.../academico/concorrencia/`, nunca em
produção — D051): a estratégia atômica já em produção (`TurmaRepository.consumirVaga`, D024) produziu
`sucessos=1, conflitos=9, excecoesInesperadas=0, tempoTotalMs=32`; a variante com
`@Lock(LockModeType.PESSIMISTIC_WRITE)` produziu `sucessos=1, conflitos=9, excecoesInesperadas=0,
tempoTotalMs=33`. Corretude idêntica entre as duas; a diferença de tempo (32ms vs. 33ms) é estatisticamente
insignificante nessa escala e execução única — ruído de medição, não sinal de performance real. Com nenhuma
vantagem mensurável demonstrada pela pessimista, e com a atômica evitando manter um lock de linha aberto
pela duração de uma transação inteira (menor risco de cadeia de espera crescer sob carga maior que a
testada aqui), a decisão final (D053) foi manter a estratégia atômica condicional de D024 — nenhuma mudança
em `src/main/java`. O harness de comparação pessimista permanece no repositório como benchmark permanente
(nunca existiu no caminho de produção, D051).

**"Como isso mudaria com muitas instituições simultâneas?"**

A contenção de vaga acontece na linha da `Turma` específica (`UPDATE ... WHERE id = ?`), não em nenhum
recurso compartilhado entre instituições — duas instituições diferentes confirmando matrículas em turmas
diferentes não competem pela mesma linha, então mais instituições, por si só, não aumentam a contenção
medida aqui. O gargalo real de escala não é "número de instituições", é volume de escrita concorrente na
*mesma* turma popular (ex: uma turma de grande procura de uma única instituição recebendo milhares de
confirmações simultâneas) — cenário de ordem de grandeza diferente do testado (20 concorrentes). A
mensageria assíncrona (D002, RabbitMQ) já isola esse caminho crítico de escrita do fluxo de
notificação/auditoria — mais instituições gerando mais eventos de domínio não pressiona a confirmação de
matrícula, porque a publicação é assíncrona e teria fila/consumidor próprios por volume. O limite honesto da
abordagem atual já está registrado no próprio D024: se a carga real de matrícula concorrente numa única
turma crescer para escala de "flash sale" (milhares de req/s pela mesma linha), o padrão a revisitar é
reserva de vaga via Redis + fila — decisão consciente de não implementar isso agora, porque a pesquisa de
mercado que embasou D024 aponta o `UPDATE` condicional como o padrão robusto certo para contenção moderada
(a realidade de capacidade de turma), não para volume de flash sale.
