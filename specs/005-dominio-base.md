# 005 — Domínio base: Aluno, Curso, Disciplina, Turma

**Status:** `concluída`
**Seção(ões) do PRD relacionadas:** §02 (entidades, cadastro/edição/listagem/exclusão), §03 (camadas, DTOs, validação, testes)
**Módulo(s) Modulith afetado(s):** novo módulo `academico` (contexto acadêmico — distinto de `security`, infraestrutura transversal).

> **Nota de formato:** por orientação explícita do usuário (prazo apertado), esta spec cobre as 4 entidades de forma condensada — uma seção por preocupação transversal (modelagem, camadas, validação, RBAC, testes), não uma seção repetida por entidade. Onde o comportamento diverge entre entidades, isso é indicado inline.

## 1. Objetivo

Implementar o CRUD completo (criar/editar/listar/excluir) de Aluno, Curso, Disciplina e Turma, com camadas (controller → service → domain → repository), DTOs na borda, validação de entrada e RBAC — sem nenhuma lógica de vaga/concorrência (isso é Fase 3/7) e sem Matrícula.

## 2. Escopo

**Dentro do escopo:**
- Entidades JPA `Aluno`, `Curso`, `Disciplina`, `Turma` no pacote `academico.domain`, com migrations Flyway correspondentes (convenção D015: pasta única, `V{seq}__academico_{descricao}.sql`).
- Relacionamento `Curso` N:N `Disciplina` (D020, tabela `curso_disciplina`); `Turma` com FK direto para `curso_id` e `disciplina_id`.
- `Turma` com `limiteVagas` (inteiro > 0) e `status` (enum `StatusTurma { ABERTA, FECHADA }`, D022) — dados prontos para a Fase 3, sem lógica de consumo/liberação de vaga implementada aqui.
- Soft delete (`ativo`, D019) nas 4 entidades — "excluir" marca `ativo=false`.
- 1 controller REST por entidade (`academico.web`), com os 4 verbos; DTOs de request/response (`academico.dto`), mapeamento manual (D018).
- Bean Validation nos DTOs de entrada; erros via `ProblemDetail`/`GlobalExceptionHandler` já existentes (spec 004) — nenhum formato de erro novo.
- RBAC: leitura (`GET`) de Curso/Disciplina/Turma aberta a qualquer autenticado (D021); escrita (`POST`/`PUT`/`DELETE`) restrita a `SECRETARIA`/`ADMIN` nas 4 entidades; Aluno (todas as operações, incluindo leitura) restrito a `SECRETARIA`/`ADMIN` nesta fase — ver seção 4.4.
- Anotações OpenAPI/springdoc (`@Tag`, `@Operation`) nos 4 controllers — o esquema Bearer já é global (spec 004), não precisa de anotação por controller.
- Testes unitários de service (regras de negócio de cada entidade) + 1 teste de integração via Testcontainers (Curso, como padrão de referência para as outras três).
- Coluna `keycloak_subject_id` (nullable, unique) na migration de `Aluno`, executando D006 (mapeamento Keycloak↔Aluno "adiado para quando Aluno existir").

**Fora do escopo (não-objetivos):**
- Matrícula, qualquer regra de vaga/concorrência (consumo/liberação) — Fase 3 (básico) / Fase 7 (aprofundamento).
- Mensageria sobre estas entidades — os eventos de domínio do PRD (`MatriculaCriada`/`Confirmada`/`Cancelada`) são todos de Matrícula.
- Paginação/filtros — diferencial do PRD §07, só entra se sobrar tempo.
- `e2e/` desta fase — só faz sentido com Matrícula (Fase 3) para um fluxo de ponta a ponta com significado real.
- Self-service de Aluno (ver própria ficha) — a coluna `keycloak_subject_id` existe, mas nenhum endpoint a consome ainda (D022).
- Auditoria completa (criado/atualizado por quem) — só um `criadoEm` (timestamp) simples por entidade, sem framework de auditoria.

## 3. Regras de negócio envolvidas

Regras do PRD §02 relevantes a esta fase (as de Matrícula/vaga ficam para a Fase 3):
- [x] Cadastro, edição, listagem e exclusão de alunos, cursos, disciplinas e turmas (PRD §03).
- [x] Turma possui limite de vagas (dado modelado, regra de consumo é Fase 3).

Regras implícitas identificadas nesta fase (D020/D022):
- [x] `Curso.codigo` e `Disciplina.codigo` únicos (case-insensitive, D023).
- [x] `Turma.codigo` único dentro do par `(curso_id, disciplina_id)` (case-insensitive, D023).
- [x] Criar `Turma` exige que `(curso_id, disciplina_id)` já exista em `curso_disciplina` — 409 caso contrário.
- [x] Não é possível inativar `Curso` com `Disciplina` ativa vinculada, nem inativar `Disciplina` com `Turma` ativa vinculada — 409 caso contrário.
- [x] Não é possível desvincular uma Disciplina de um Curso se houver `Turma` (ativa ou inativa) referenciando aquele par — 409 caso contrário (achado de review, D023).

## 4. Abordagem técnica

### 4.1 Modelagem e migrations

Pacote `br.com.desafio.tecnico.gestao.academico` (novo módulo Modulith), subpacotes `domain`, `repository`, `service`, `dto`, `web`.

Migrations (`V1`–`V5`, ordem por dependência de FK):
1. `V1__academico_criar_tabela_aluno.sql` — `id` (bigserial), `nome`, `email` (unique, not null), `keycloak_subject_id` (nullable, unique), `ativo` (boolean, default true), `criado_em`.
2. `V2__academico_criar_tabela_curso.sql` — `id`, `codigo` (unique), `nome`, `ativo`, `criado_em`.
3. `V3__academico_criar_tabela_disciplina.sql` — `id`, `codigo` (unique), `nome`, `ativo`, `criado_em`.
4. `V4__academico_criar_tabela_curso_disciplina.sql` — tabela de junção, `curso_id`+`disciplina_id` (FK, PK composta).
5. `V5__academico_criar_tabela_turma.sql` — `id`, `codigo`, `curso_id` (FK), `disciplina_id` (FK), `limite_vagas` (int, > 0), `status` (varchar, enum), `ativo`, `criado_em`; unique `(curso_id, disciplina_id, codigo)`.

### 4.2 Camadas e DTOs

- `domain`: `Aluno`, `Curso`, `Disciplina`, `Turma` (entidades JPA), `StatusTurma` (enum).
- `repository`: `AlunoRepository`, `CursoRepository`, `DisciplinaRepository`, `TurmaRepository` (Spring Data JPA), todos com um finder que já filtra `ativo=true` para as listagens.
- `service`: uma classe por entidade, com as regras de negócio da seção 3 (validação de unicidade, bloqueio de inativação com filhos ativos, validação do par curso/disciplina em Turma).
- `dto`: `{Entidade}Request` (entrada, com Bean Validation) e `{Entidade}Response` (saída) por entidade — mapeamento manual (D018), sem MapStruct.
- `web`: um `@RestController` por entidade, endpoints `POST /api/{entidade}s`, `PUT /api/{entidade}s/{id}`, `GET /api/{entidade}s`, `GET /api/{entidade}s/{id}`, `DELETE /api/{entidade}s/{id}` (soft delete, 204).

Reaproveitamento de spec 004: `RecursoNaoEncontradoException` (404, ex: id inexistente) e `ConflitoRegraNegocioException` (409, ex: código duplicado, par curso/disciplina inválido, inativação bloqueada) — nenhuma exceção nova.

**Lacuna descoberta durante a implementação (não prevista neste rascunho original):** nada no desenho
acima populava a tabela `curso_disciplina` — sem isso, toda criação de `Turma` falharia sempre (o par
`curso_id`/`disciplina_id` nunca estaria associado, D020). Corrigido adicionando dois endpoints extras em
`CursoController`/`CursoService` (mesmo RBAC de escrita, `SECRETARIA`/`ADMIN`):
- `POST /api/cursos/{id}/disciplinas/{disciplinaId}` — associa uma Disciplina ativa a um Curso ativo
  (`CursoService.vincularDisciplina`), 200, devolve o `CursoResponse` atualizado.
- `DELETE /api/cursos/{id}/disciplinas/{disciplinaId}` — remove a associação (`CursoService.desvincularDisciplina`),
  204; bloqueada com 409 se houver `Turma` (ativa ou inativa) referenciando aquele par — achado do
  `code-reviewer`/`security-auditor` (ver [D023](../docs/DECISIONS.md#d023)): sem essa checagem, a remoção
  violava a FK composta de `turma` e vazava um 500 genérico em vez do 409 de negócio padrão da API.

### 4.3 Testes

- **Testcontainers** (novo nesta fase — `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-junit-jupiter` no `pom.xml`): 1 teste de integração para `Curso` (`CursoIntegrationTest`), cobrindo criar → listar → editar → excluir via `MockMvc` contra um Postgres real via Testcontainers (não a mesma instância do `docker compose` de desenvolvimento) — como pede o `CLAUDE.md` para testes de integração.
- Testes unitários de service para as 4 entidades (Mockito, sem Spring context): regra de unicidade, entidade não encontrada, bloqueio de inativação com filhos ativos (Curso/Disciplina), validação do par curso/disciplina (Turma).

### 4.4 RBAC

- `Curso`/`Disciplina`/`Turma`: `GET` (`hasAuthority`/`hasRole` qualquer papel autenticado — sem `@PreAuthorize` explícito, já que a cadeia principal exige autenticação por padrão); `POST`/`PUT`/`DELETE` com `@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")` (D021).
- `Aluno`: todas as operações (incluindo `GET`) com `@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")` — não decidido via pergunta formal (só Curso/Disciplina/Turma foram perguntados), mas mantém a mesma razão: consultar/listar dados de outros alunos não é algo que um `ALUNO` deveria poder fazer livremente (privacidade), e self-view não está no escopo desta fase (D022). Revisitar quando a Fase 3 definir o que o próprio aluno precisa enxergar.

## 5. Decisões que requerem confirmação antes de implementar

| # | Pergunta | Alternativas consideradas | Decisão (link) |
|---|---|---|---|
| 1 | MapStruct ou mapeamento manual? | MapStruct vs. manual | [D018](../docs/DECISIONS.md#d018) — manual |
| 2 | Soft delete ou hard delete? | Hard delete vs. soft delete | [D019](../docs/DECISIONS.md#d019) — soft delete |
| 3 | Disciplina pertence a 1 Curso ou é compartilhada? | 1:N vs. N:N | [D020](../docs/DECISIONS.md#d020) — N:N |
| 4 | Quem lê Curso/Disciplina/Turma? | Só staff vs. qualquer autenticado | [D021](../docs/DECISIONS.md#d021) — qualquer autenticado |

Decisões menores agrupadas em [D022](../docs/DECISIONS.md#d022) (status de Turma, unicidade, vínculo Aluno↔Keycloak) — sinalizadas para revisão, não bloqueantes.

## 6. Critérios de aceite

- [x] Os 4 controllers expõem criar/editar/listar/excluir, retornando DTOs (nunca entidade JPA).
- [x] Validação de entrada (Bean Validation) retorna 400 via `ProblemDetail` já existente, com `errorCode=VALIDATION_ERROR`.
- [x] Entidade não encontrada retorna 404 (`RecursoNaoEncontradoException`); código duplicado/par curso-disciplina inválido/inativação bloqueada retornam 409 (`ConflitoRegraNegocioException`).
- [x] `GET` de Curso/Disciplina/Turma funciona com token de qualquer papel (`ALUNO`/`SECRETARIA`/`ADMIN`); `POST`/`PUT`/`DELETE` retornam 403 para `ALUNO`. Validado com tokens reais do Keycloak (`aluno.teste`/`secretaria.teste`/`admin.teste`).
- [x] Todas as operações de Aluno retornam 403 para token `ALUNO`, 200/201/204 para `SECRETARIA`/`ADMIN`. Validado com tokens reais.
- [x] Excluir marca `ativo=false` (soft delete) — registro não aparece mais em listagens, mas continua no banco.
- [x] `/swagger-ui.html` mostra os 4 controllers agrupados por `@Tag`.
- [x] Testes unitários de service cobrindo as regras de negócio da seção 3.
- [x] `CursoIntegrationTest` (Testcontainers) passa: criar → listar → editar → excluir.
- [x] Cobertura ≥ 80% no módulo `academico` (gate JaCoCo, spec 004). Resultado: 85,2% linha / 92,3% branch.
- [x] `code-reviewer` e `security-auditor` executados, achados endereçados (ver [D023](../docs/DECISIONS.md#d023)).
- [x] `docs/DECISIONS.md` contém D018–D023.

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | Regras de negócio de cada service (unicidade, não encontrado, bloqueio de inativação, par curso/disciplina inválido) | `academico/service/*Test.java` |
| Integração | Criar → listar → editar → excluir de `Curso`, via `MockMvc` + Testcontainers (Postgres real) | `academico/CursoIntegrationTest.java` |
| E2E | N/A nesta fase — ver seção 2 | — |

**Justificativa de cobertura:** as 4 entidades seguem o mesmo padrão de camada; testar as regras de negócio reais de cada service (não getter/setter) e validar o fluxo completo de uma entidade via Testcontainers (padrão a ser copiado para as outras três em revisão futura, se necessário) cobre o que realmente importa: que a regra de negócio funciona e que a integração com o banco real funciona.

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** nenhuma mudança além do já existente (logs estruturados via specs/002 continuam cobrindo estas rotas automaticamente).
- **Segurança:** RBAC aplicado de forma consistente nos 4 controllers (achado explícito a verificar no security review); nenhum dado sensível novo (Aluno tem `email`, mas RBAC já restringe a `SECRETARIA`/`ADMIN`); validação de entrada em 100% dos DTOs de request.

## 9. Definition of Done desta tarefa

- [x] Critérios de aceite (seção 6) atendidos
- [x] Testes da seção 7 implementados e passando (67/67 testes do projeto, incluindo os desta fase)
- [x] Cobertura ≥ 80% no módulo `academico` (85,2% linha / 92,3% branch)
- [x] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5, mais D023 (achados de review)
- [x] `code-reviewer` executado — 5 achados, todos endereçados ou descartados com justificativa (D023)
- [x] `security-auditor` executado — 1 achado (o mesmo item #1 do code-reviewer, severidade divergente), endereçado (D023)
- [x] Esta spec atualizada para refletir o que foi de fato implementado (endpoints de vínculo Curso↔Disciplina, seção 4.2; correções de review, seção abaixo)
- [x] `./mvnw clean verify` passando

## 10. Pronto para Fase 3

`Turma` já tem os dois campos que a Fase 3 (Matrícula) precisa para implementar a regra de vagas, sem
necessidade de migration adicional:
- `limite_vagas` (`int`, `CHECK > 0`, `NOT NULL`) — a Fase 3 implementa o consumo/liberação de vaga em cima
  deste valor (contagem de matrículas `CONFIRMADA` vs. este limite, ou um contador redundante — decisão a
  tomar na spec da Fase 3, incluindo a estratégia de concorrência, PRD §02).
- `status` (enum `StatusTurma { ABERTA, FECHADA }`) — pronto para a Fase 3 usar como um dos critérios de
  "aluno só pode se matricular em turma aberta" (PRD §02), embora hoje nenhum código transicione esse
  enum automaticamente (fica `ABERTA` desde a criação; a Fase 3 decide se a transição para `FECHADA` é
  manual, automática ao esgotar vagas, ou ambas).

`Aluno` também está pronto: `keycloak_subject_id` (nullable, unique) já existe desde a V1 (executa D006),
para quando a Fase 3 precisar resolver "qual Aluno corresponde a este token JWT" no fluxo de auto-matrícula.

Nenhuma migration adicional é necessária para a Fase 3 começar — a tabela `matricula` em si (nova) é o
único schema pendente, a ser desenhado na spec correspondente.

## 11. Correções de code review / security review (fechamento)

`code-reviewer` e `security-auditor` revisaram o diff completo desta spec em paralelo (etapas 7/8 do
CLAUDE.md). Resumo dos achados e resolução — detalhes completos em
[D023](../docs/DECISIONS.md#d023):

1. **Corrigido:** `desvincularDisciplina` sem guarda contra `Turma` vinculada ao par — causava 500 genérico
   em vez de 409 de negócio (achado convergente dos dois revisores, severidade HIGH/LOW divergente).
2. **Corrigido:** unicidade case-sensitive em `Aluno.email`/`Curso.codigo`/`Disciplina.codigo`/`Turma.codigo`
   — normalização (`toLowerCase`/`toUpperCase` + `trim`) adicionada na borda dos services.
3. **Corrigido:** N+1 ao listar `Turma` — `@EntityGraph` adicionado em `TurmaRepository`.
4. **Corrigido:** status HTTP implícito em `POST /api/cursos/{id}/disciplinas/{disciplinaId}` — `@ResponseStatus(HttpStatus.OK)` explícito.
5. **Descartado com justificativa (risco documentado, não corrigido):** `Curso`/`Disciplina` sem
   `equals`/`hashCode` customizado como elementos de `Set` — não é um bug hoje alcançável por nenhum
   caminho de código existente; custo de implementar corretamente sob o prazo desta fase não se justifica
   agora. Ver D023 para o raciocínio completo e o gatilho de quando revisitar.

Todas as correções foram validadas com o build completo (`./mvnw clean verify`, 67/67 testes) e as duas
correções de maior risco (item 1 e item 2) também foram validadas manualmente ponta a ponta com a
aplicação rodando e tokens reais do Keycloak (409 em vez de 500 na desvinculação com Turma associada;
409 ao criar Curso com código igual em outra caixa).
