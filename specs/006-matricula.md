# 006 — Matrícula: entidade, regras de negócio e proteção de vaga

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §02 (entidades/regras de negócio, especialmente limite de vagas —
critério eliminatório do §06), §03 (camadas, DTOs, validação, testes)
**Módulo(s) Modulith afetado(s):** `academico` (entidade `Matricula`, alteração em `Turma`); novo módulo
`notificacao` (consumidor de eventos de domínio)

## 1. Objetivo

Implementar a entidade `Matrícula` e o fluxo completo de negócio sobre ela — criar, confirmar, cancelar,
consultar — com proteção de vaga correta sob concorrência (lock otimista via `@Version`, sem a análise
comparativa aprofundada de estratégias, que é Fase 7) e publicação interna de eventos de domínio via Spring
Modulith (sem broker externo ainda, que é Fase 4). É a fase mais crítica do desafio: PRD §06 lista a regra
de vagas como critério eliminatório.

## 2. Escopo

**Dentro do escopo:**
- Entidade `Matricula` (`aluno`, `turma`, `status`, timestamps de criação/confirmação/cancelamento) e
  migration Flyway correspondente.
- Alteração em `Turma`: campo `vagasOcupadas` (int) e `@Version` (D024/D025).
- Todas as regras de negócio do PRD §02 sobre Matrícula, com teste cobrindo cada uma (seção 3).
- Proteção de vaga via `UPDATE` condicional atômico + `@Version` (D024), com teste de concorrência real
  (duas requisições simultâneas disputando a última vaga).
- Eventos de domínio (`MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada`) publicados via
  `ApplicationEventPublisher`, consumidos por um `@ApplicationModuleListener` no novo módulo `notificacao`
  (D029 — sem RabbitMQ ainda).
- API REST: criar, confirmar, cancelar, consultar por id/aluno/turma.
- RBAC (papéis já existentes) + ABAC (extensão do `PermissionEvaluator` da spec 003 — primeiro uso real).
- Vínculo Aluno↔Keycloak preenchível via cadastro de Aluno (D030) — necessário para ABAC funcionar de
  verdade.
- Teste E2E via `e2e/matricula-flow.sh` (D030), novo step no `ci.yml`.
- Teste `ApplicationModules.verify()` (Spring Modulith) — primeira vez que o projeto tem mais de um módulo
  com interação real entre eles.

**Fora do escopo (não-objetivos):**
- RabbitMQ como broker externo — Fase 4 (D029).
- Análise comparativa de estratégias de concorrência (otimista vs. pessimista vs. constraint), testes de
  carga, tuning — Fase 7, deliberadamente.
- Reserva de vaga com TTL / Redis / fila — desproporcional à escala real deste sistema (D024); revisitar só
  se a Fase 7 indicar necessidade.
- Paginação/filtros nas consultas — diferencial do PRD §07.
- Self-service de vínculo Aluno↔Keycloak (o próprio aluno reivindicar seu registro) — só staff vincula
  (D030).
- Frontend — Fase 5.
- Auditoria de acesso de staff a registro alheio (pendência já registrada em specs/003, seção 4.3, M3) —
  não resolvida nesta fase; ver seção 10.

## 3. Regras de negócio envolvidas

- [x] Aluno só pode ser matriculado em turma `ABERTA` (PRD §02).
- [x] Turma tem limite de vagas; confirmar matrícula consome vaga; cancelar matrícula `CONFIRMADA` libera
  vaga (PRD §02, §06 — crítico).
- [x] Aluno não pode se matricular duas vezes na mesma turma **entre matrículas `PENDENTE`/`CONFIRMADA`**;
  uma matrícula `CANCELADA` não bloqueia nova matrícula (D026).
- [x] Máquina de estados completa (D028): `PENDENTE→CONFIRMADA`, `PENDENTE→CANCELADA`,
  `CONFIRMADA→CANCELADA`, `CONFIRMADA→CONFIRMADA` (idempotente, não-erro); `CANCELADA` é terminal (qualquer
  transição a partir dela é 409).
- [x] Duas requisições concorrentes disputando a última vaga: exatamente uma confirma, a outra recebe 409
  (D024, PRD §06 — critério eliminatório). Validado por `MatriculaConcorrenciaIntegrationTest`
  (`@RepeatedTest(10)`) rodado em 7 invocações separadas (70 execuções no total), zero falhas.
- [x] Consulta de matrículas por aluno e por turma (sem paginação obrigatória nesta fase).

## 4. Abordagem técnica

### 4.1 Modelagem e migrations

Migrations novas (`V6`, `V7`, convenção D015):
1. `V6__academico_criar_tabela_matricula.sql` — `id`, `aluno_id` (FK), `turma_id` (FK), `status`
   (`varchar`, enum `StatusMatricula { PENDENTE, CONFIRMADA, CANCELADA }`), `criado_em`, `confirmado_em`
   (nullable), `cancelado_em` (nullable). Índice único parcial (D026):
   `CREATE UNIQUE INDEX uk_matricula_aluno_turma_ativa ON matricula (aluno_id, turma_id) WHERE status <> 'CANCELADA'`.
2. `V7__academico_alterar_turma_vagas_ocupadas.sql` — `ALTER TABLE turma ADD COLUMN vagas_ocupadas INT NOT NULL DEFAULT 0`,
   `ADD COLUMN version BIGINT NOT NULL DEFAULT 0`, `ADD CONSTRAINT ck_turma_vagas_ocupadas_valida CHECK (vagas_ocupadas >= 0 AND vagas_ocupadas <= limite_vagas)`
   (D025).
3. **Não prevista no rascunho original:** `V8__infraestrutura_criar_tabela_event_publication.sql` — a
   publicação de eventos (seção 4.3) só se tornou real nesta fase; `spring-modulith-events-jpa` (já no
   `pom.xml` desde a Fase 1) precisa de uma tabela própria (`event_publication`) para seu registro de
   publicação, e nada a havia migrado até agora. Schema copiado literalmente de
   `spring-modulith-events-jdbc:1.4.12` (`schema-postgresql.sql`) — verificado byte-a-byte pelo
   code-reviewer contra o jar oficial.

`Turma` (entidade existente, `academico.domain`) ganha `vagasOcupadas` (int) e `@Version private Long version`.

`Matricula` (nova, `academico.domain`): `@ManyToOne` para `Aluno` e `Turma` (ambos `LAZY`, `NOT NULL`),
`status` (`@Enumerated(STRING)`), `criadoEm`/`confirmadoEm`/`canceladoEm` (`Instant`, os dois últimos
nullable). Sem soft delete — matrícula cancelada é um estado, não uma exclusão (diferente do padrão D019 de
Aluno/Curso/Disciplina/Turma).

### 4.2 Camadas e DTOs

- `domain`: `Matricula`, `StatusMatricula` (enum).
- `repository`: `MatriculaRepository` — `findByAlunoId`, `findByTurmaId`, `findById` (redeclarado com
  `@EntityGraph(attributePaths={"aluno","turma"})` para evitar N+1, achado de code review, D031),
  `existsByAlunoIdAndTurmaIdAndStatusNot(alunoId, turmaId, CANCELADA)` (checagem de unicidade em código,
  além do índice único parcial no banco — mesmo padrão de dupla garantia já usado em D020/D023),
  `findAlunoKeycloakSubjectIdByMatriculaId` (projeção escalar para ABAC, ver seção 4.5).
  `TurmaRepository` ganha `@Modifying(clearAutomatically=true, flushAutomatically=true) @Query` para o
  `UPDATE` condicional atômico (D024):
  `UPDATE Turma t SET t.vagasOcupadas = t.vagasOcupadas + 1, t.version = t.version + 1 WHERE t.id = :id AND t.version = :version AND t.vagasOcupadas < t.limiteVagas`
  (retorna `int`, linhas afetadas) e o equivalente para liberar vaga (`vagasOcupadas - 1`, sem a condição de
  limite, só `vagasOcupadas > 0` como salvaguarda).
- `dto`: `MatriculaRequest` (`alunoId`, `turmaId`), `MatriculaResponse` (id, alunoId, alunoNome, turmaId,
  turmaCodigo, status, os 3 timestamps). `TurmaResponse` ganhou o campo `vagasOcupadas` (não existia na
  Fase 2 — necessário para o cliente/testes observarem consumo de vaga).
- `service`: `MatriculaService` — `criar`, `confirmar`, `cancelar`, `buscarPorId`, `listarPorAluno`,
  `listarPorTurma`. Reaproveita `RecursoNaoEncontradoException`/`ConflitoRegraNegocioException`
  (`VAGAS_ESGOTADAS`, `CONFLITO_CONCORRENCIA`, `TRANSICAO_INVALIDA`, `MATRICULA_DUPLICADA` como `errorCode`
  — nenhuma exceção nova, só códigos novos). Publica os 3 eventos via `ApplicationEventPublisher` no
  momento exato de cada transição bem-sucedida (D029).
- `web`: `MatriculaController` — `POST /api/matriculas`, `POST /api/matriculas/{id}/confirmar`,
  `POST /api/matriculas/{id}/cancelar`, `GET /api/matriculas/{id}`. **Divergência da spec original
  (achado de code review, D031):** `GET /api/alunos/{alunoId}/matriculas` e
  `GET /api/turmas/{turmaId}/matriculas` foram implementados em `AlunoController`/`TurmaController` (não em
  `MatriculaController`) — melhor aninhamento REST (o recurso pai já é dono do path), mesma decisão de
  RBAC/ABAC descrita na seção 4.5.

### 4.3 Eventos de domínio e módulo `notificacao`

Records simples: `MatriculaCriada(Long matriculaId, Long alunoId, Long turmaId)`,
`MatriculaConfirmada(Long matriculaId)`, `MatriculaCancelada(Long matriculaId, boolean liberouVaga)`.
**Ajuste em relação ao rascunho original:** ficam no pacote raiz de `academico` (não em `academico.event`
como cogitado) — convenção do Spring Modulith: um tipo só é automaticamente parte da API pública de um
módulo (consumível por `notificacao` sem `@NamedInterface`) se estiver no pacote raiz; tipos em subpacotes
são internos por padrão. Mesmo motivo levou a criar `AlunoOwnershipResolver` (também pacote raiz de
`academico`) como API mínima para o `PerfilPermissionEvaluator` do módulo `security` resolver posse sem
enxergar `academico.repository`. `MatriculaService` publica via `ApplicationEventPublisher.publishEvent(...)`
depois que a transação principal é bem-sucedida (não antes — evita publicar evento de algo que pode dar
rollback); como o listener usa `@ApplicationModuleListener` (que por padrão só entrega após o commit da
transação), há uma segunda camada de garantia contra publicar eventos de operações que acabam revertidas.

Novo módulo top-level `notificacao` (Spring Modulith trata cada subpacote direto de `gestao` como módulo,
CLAUDE.md): um `@Component` com métodos `@ApplicationModuleListener` reagindo a pelo menos
`MatriculaConfirmada` (mínimo pedido), logando de forma estruturada "notificação seria enviada aqui —
matrícula {id} confirmada" — sem lógica real de notificação (fora de escopo, D029).

Teste novo: `ApplicationModules.of(GestaoApplication.class).verify()` (`src/test/.../ModularidadeTest.java`
ou similar) — primeira vez que isso entra no projeto, já que antes só existia um módulo com interação
relevante (`academico`+`security`+`errorhandling` não têm eventos entre si).

### 4.4 Proteção de vaga (D024/D025/D028)

`MatriculaService.confirmar(id)`:
1. Carrega a `Matricula` (404 se não existir); valida transição (`PENDENTE→CONFIRMADA` ou
   `CONFIRMADA→CONFIRMADA` idempotente — retorna sem novo efeito; qualquer outro estado de origem é 409
   `TRANSICAO_INVALIDA`).
2. Se a transição for `PENDENTE→CONFIRMADA`: chama o `UPDATE` condicional atômico em `Turma` (seção 4.2).
   - 1 linha afetada → sucesso: atualiza `Matricula.status=CONFIRMADA`, `confirmadoEm=now()`, publica
     `MatriculaConfirmada`.
   - 0 linhas afetadas → re-consulta a `Turma` atual: se `vagasOcupadas >= limiteVagas`, é
     `VAGAS_ESGOTADAS` (409); caso contrário, foi conflito de versão por edição concorrente de outro campo
     (`CONFLITO_CONCORRENCIA`, 409) — ver D028 para o porquê de não haver retry automático em nenhum dos
     dois casos.

`MatriculaService.cancelar(id)`: se a matrícula estava `CONFIRMADA`, chama o `UPDATE` de liberação de vaga
(sempre "tem sucesso" no sentido de negócio — não há disputa por *liberar*, só por *consumir*; ainda
protegido por `@Version` contra edição concorrente de outro campo, tratado como retry transparente de uma
tentativa nesse caso raro específico, já que liberar vaga não tem a mesma preocupação de "furar fila" que
consumir tem).

`GlobalExceptionHandler` ganha um `@ExceptionHandler(OptimisticLockingFailureException.class)` como rede de
segurança (caso alguma operação futura use `save()` direto em vez do `UPDATE` condicional) — mapeia para
409 com mensagem genérica ("Conflito de concorrência, tente novamente.", `errorCode=CONCURRENCY_CONFLICT`),
**sem vazar detalhes internos do lock** (nome da exceção, número de versão, etc.) — atenção explícita
pedida para o security review, verificada por teste dedicado.

**Achado de code review (D031), corrigido após implementação inicial incorreta:** `confirmar`/`cancelar`
NÃO usam `matriculaRepository.save(matricula)` para persistir a transição de status. O
`clearAutomatically=true` do `UPDATE` condicional detacha `matricula` do persistence context; `save()`
faria `merge()`, que sem `cascade=MERGE` na associação recria `aluno`/`turma` como proxies não
inicializados — reintroduzindo N+1 ao montar `MatriculaResponse` (mascarado só por
`spring.jpa.open-in-view=true`, nunca desabilitado neste projeto). Uma primeira tentativa de correção
(re-buscar via `findById` com `@EntityGraph` após o `save()`) não funcionava de verdade — o identity map do
Hibernate devolve a instância já em cache (recém-mergeada), sem re-executar a query com o graph. A correção
efetiva: recarregar uma instância MANAGED via `buscarPorId` (que usa `@EntityGraph`) **antes** de mutar os
campos, e deixar o dirty checking do Hibernate gerar o `UPDATE` no commit — sem `merge()`, `aluno`/`turma`
nunca são substituídos por proxies. Verificado empiricamente com log de SQL (`org.hibernate.SQL=DEBUG`)
contra o Testcontainers: zero queries extras após o `UPDATE`.

**Também achado de review (D031):** as checagens de unicidade em código (`existsBy...`, ex: duplicidade de
matrícula) são "checar depois agir" (TOCTOU) — sob concorrência real (ex: duplo clique), ambas podem passar
a checagem e uma violar a constraint no banco (índice único parcial, backstop de D026). `GlobalExceptionHandler`
ganhou um `@ExceptionHandler(DataIntegrityViolationException.class)` genérico (409,
`errorCode=DATA_INTEGRITY_CONFLICT`, sem detalhe de constraint/tabela) como rede de segurança para essa
janela de corrida em qualquer service do projeto, não só Matrícula.

### 4.5 RBAC/ABAC (D027/D030)

- `POST /api/matriculas`: `ALUNO` (só para si mesmo, `#request.alunoId()` via ABAC), `SECRETARIA`/`ADMIN`
  (para qualquer aluno). `@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN') or hasPermission(#request.alunoId(), 'ALUNO', 'MATRICULAR')")`.
- `POST /api/matriculas/{id}/confirmar`: só `SECRETARIA`/`ADMIN` (D027) — `hasRole` puro, sem ABAC.
- `POST /api/matriculas/{id}/cancelar`: `ALUNO` (só a própria), `SECRETARIA`/`ADMIN` (qualquer) — ABAC via
  `hasPermission(#id, 'MATRICULA', 'GERENCIAR')`.
- `GET /api/matriculas/{id}`: mesma regra de `cancelar` (ABAC `MATRICULA`/`GERENCIAR`) — usa a mesma
  permissão para ler e cancelar, já que a spec 003 não distinguiu granularidade de leitura vs. escrita para
  posse (mesmo padrão do exemplo `PERFIL`).
- `GET /api/alunos/{alunoId}/matriculas`: `ALUNO` (só as próprias, ABAC `hasPermission(#alunoId, 'ALUNO', 'MATRICULAR')`),
  `SECRETARIA`/`ADMIN` (qualquer aluno).
- `GET /api/turmas/{turmaId}/matriculas`: só `SECRETARIA`/`ADMIN` (D030) — `hasRole` puro.

`PerfilPermissionEvaluator` (mantém o nome atual, já que continua tratando `PERFIL` também) ganha dois
`targetType` novos (`ALUNO`, `MATRICULA`). **Ajuste em relação ao rascunho original:** não injeta
`AlunoRepository`/`MatriculaRepository` diretamente — esses tipos são internos de `academico.repository`,
e o módulo `security` não pode enxergá-los sem violar o encapsulamento de módulo do Spring Modulith
(`ApplicationModules.verify()` acusaria a violação). Em vez disso, injeta `AlunoOwnershipResolver`
(`academico`, pacote raiz — API pública mínima do módulo), que expõe só `keycloakSubjectIdDoAluno(Long)` e
`keycloakSubjectIdDoAlunoDaMatricula(Long)`, cada um via uma projeção escalar (`SELECT a.keycloakSubjectId
FROM Aluno a WHERE ...`) para não carregar a entidade inteira nem arriscar `LazyInitializationException`.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | Mecanismo de consumo de vaga | `UPDATE` condicional atômico vs. carregar→checar em Java→`save()` | [D024](../docs/DECISIONS.md#d024) — `UPDATE` condicional |
| 2 | Contador de vagas ocupadas | Campo incremental vs. `COUNT` calculado | [D025](../docs/DECISIONS.md#d025) — campo incremental |
| 3 | Matrícula cancelada bloqueia nova matrícula? | Sim vs. não | [D026](../docs/DECISIONS.md#d026) — não bloqueia |
| 4 | Quem confirma matrícula | Só staff vs. também o próprio aluno | [D027](../docs/DECISIONS.md#d027) — só staff |

Decisões menores agrupadas em [D028](../docs/DECISIONS.md#d028) (máquina de estados completa, resposta a
conflito de vaga), [D029](../docs/DECISIONS.md#d029) (sequenciamento de mensageria — já decidida pelo
Pablo, registrada aqui) e [D030](../docs/DECISIONS.md#d030) (extensão do ABAC, vínculo Aluno↔Keycloak, RBAC
de listagem por turma, ferramenta de E2E) — sinalizadas para revisão, não bloqueantes.
[D031](../docs/DECISIONS.md#d031) documenta as correções decorrentes do code-reviewer/security-auditor
(fechamento da spec).

## 6. Critérios de aceite

- [x] `POST /api/matriculas` cria matrícula `PENDENTE`; 409 se aluno já tem matrícula `PENDENTE`/
  `CONFIRMADA` na mesma turma; 409 se turma não está `ABERTA`; 403 se `ALUNO` tentar matricular outro aluno.
- [x] `POST /api/matriculas/{id}/confirmar` consome vaga, seta `confirmadoEm`; 409 `VAGAS_ESGOTADAS` se
  não há vaga; 200 idempotente se já `CONFIRMADA`; 409 `TRANSICAO_INVALIDA` se `CANCELADA`; 403 se
  `ALUNO`.
- [x] `POST /api/matriculas/{id}/cancelar` libera vaga só se estava `CONFIRMADA`, seta `canceladoEm`; 409
  se já `CANCELADA`; 403 se `ALUNO` tentando cancelar matrícula de outro aluno.
- [x] Duas requisições concorrentes confirmando a última vaga de uma turma: exatamente uma recebe 200/
  `CONFIRMADA`, a outra recebe 409 `VAGAS_ESGOTADAS` — comprovado por teste de concorrência real (threads/
  requisições simultâneas), rodado 7 vezes separadas (70 execuções) sem flakiness.
- [x] `GET /api/matriculas/{id}`, `GET /api/alunos/{alunoId}/matriculas`: `ALUNO` só acessa as próprias
  (403 em cruzado); `SECRETARIA`/`ADMIN` acessam qualquer uma. Validado com tokens simulados (testes de
  integração) e reais (RBAC manual, seção 19 do processo).
- [x] `GET /api/turmas/{turmaId}/matriculas`: 403 para `ALUNO`, 200 para `SECRETARIA`/`ADMIN`.
- [x] Os 3 eventos de domínio são publicados nos momentos corretos; o listener em `notificacao` reage a
  `MatriculaConfirmada` (e também a `MatriculaCriada`/`MatriculaCancelada`) sem acoplamento direto de código
  entre os módulos.
- [x] `ApplicationModules.verify()` passa (limites entre módulos respeitados).
- [x] Erro de conflito de vaga não vaza detalhe interno do lock otimista (nome de exceção, número de
  versão) na resposta HTTP — verificado por teste dedicado.
- [x] `e2e/matricula-flow.sh` cobre criar → confirmar → duplicidade (falha) → cancelar → vaga liberada,
  contra tokens reais do Keycloak; integrado ao `ci.yml`. Rodado manualmente 3 vezes seguidas (idempotente).
- [x] Cobertura ≥ 80% no módulo `academico` (91,3% linha / 96,2% branch) e no módulo `notificacao` (100%).
- [x] `code-reviewer` e `security-auditor` executados, achados endereçados (D031).
- [x] `docs/DECISIONS.md` contém D024–D031.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Cada regra de negócio da seção 3 isoladamente (Mockito): turma fechada, duplicidade, transições válidas/inválidas, idempotência de confirmação, vagas esgotadas | `academico/service/MatriculaServiceTest.java` |
| Integração | Fluxo completo criar→confirmar→cancelar com verificação de `vagasOcupadas` da Turma a cada passo; ABAC (aluno A não acessa matrícula de aluno B, 403); teste de concorrência real (2 threads/requisições simultâneas disputando a última vaga, via Testcontainers) | `academico/MatriculaIntegrationTest.java`, `academico/MatriculaConcorrenciaIntegrationTest.java` |
| Módulo | `ApplicationModules.verify()` | `ModularidadeTest.java` (pacote raiz de teste) |
| E2E | criar → confirmar → duplicidade (deve falhar) → cancelar → vaga liberada, via HTTP real e tokens reais do Keycloak | `e2e/matricula-flow.sh` |

**Justificativa de cobertura:** o teste de concorrência é o item que prova o critério eliminatório do PRD
§06 — roda isolado (Turma com 1 vaga, 2 requisições simultâneas) e é executado múltiplas vezes seguidas
antes de fechar a spec, especificamente para descartar flakiness (uma race condition que "às vezes passa"
não é prova de nada). Os demais testes cobrem cada regra de negócio individualmente (não só linha) e o
primeiro caso real de ABAC do projeto (a spec 003 só tinha o exemplo `PERFIL`).

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** o listener em `notificacao` loga de forma estruturada (specs/002) a reação a
  `MatriculaConfirmada` — sem dado sensível (só ids). Nenhuma métrica nova nesta fase (fica para quando
  houver necessidade real de observar taxa de conflito de vaga, possivelmente Fase 7).
- **Segurança:** primeiro caso real de ABAC do projeto (aluno só acessa a própria matrícula) — atenção
  específica no security review para acesso cruzado entre alunos. Mensagem de erro de conflito de vaga não
  pode vazar detalhe interno do lock otimista (D024, seção 4.4). Validação de entrada em `MatriculaRequest`
  (`alunoId`/`turmaId` `@NotNull`).

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando, incluindo o de concorrência rodado múltiplas vezes sem
  flakiness
- [x] Cobertura ≥ 80% nos módulos `academico` e `notificacao`
- [x] `docs/DECISIONS.md` atualizado com D024–D031
- [x] `code-reviewer` executado — achados endereçados (D031) ou justificadamente descartados
- [x] `security-auditor` executado — atenção especial a ABAC cruzado entre alunos e não-vazamento de
  detalhe de lock otimista, nenhum achado Crítico/Alto
- [x] Esta spec atualizada para refletir o que foi de fato implementado
- [x] `./mvnw clean verify` passando (119/119 testes)

## 10. Pendências para Fase 7

Itens deliberadamente simplificados nesta fase, para retomar na Fase 7 (análise comparativa aprofundada de
concorrência, PRD §06):

- **Comparação com lock pessimista (`SELECT ... FOR UPDATE`) e outras estratégias.** Esta fase implementou
  só o `UPDATE` condicional atômico (D024) — correto e testado sob concorrência real, mas sem comparação
  medida contra pessimista, sem constraint-only, sem reserva com TTL. A pesquisa que embasou D024 já
  identificou quando cada abordagem se paga (ver D024 em docs/DECISIONS.md) — a Fase 7 é para medir, não
  para decidir do zero.
- **Testes de carga com mais threads/mais contenção.** `MatriculaConcorrenciaIntegrationTest` prova
  corretude com 2 threads disputando 1 vaga (o mínimo aceitável para o critério eliminatório) — não mede
  throughput/latência sob dezenas ou centenas de requisições simultâneas.
- **Reserva de vaga com TTL / Redis / fila para escala de "flash sale".** Descartado como desproporcional à
  escala real deste sistema (D024) — revisitar só se a Fase 7 indicar necessidade real medida, não por
  padrão.
- **Auditoria de acesso de staff a registro alheio** (pendência já registrada em specs/003, seção 4.3, M3)
  — continua não resolvida. Agora com dado real (Matrícula) para o cenário fazer sentido: hoje, quando
  SECRETARIA/ADMIN acessa a matrícula de um aluno via o override de staff no ABAC, nenhuma trilha de
  auditoria distingue isso de um acesso ao próprio recurso. Necessário para detectar uso indevido de
  privilégio, não só bloqueio indevido.
- **Camada de service sem checagem de autorização independente do `@PreAuthorize` do controller** (achado
  de security review, D031, descartado por baixo risco atual) — se um futuro chamador interno (job
  agendado, outro listener) invocar `MatriculaService` diretamente, bypassa ABAC silenciosamente. Revisitar
  se/quando esse tipo de chamador surgir.
