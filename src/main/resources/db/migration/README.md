A Fase 1 (`specs/001` a `specs/004`) cobriu só infraestrutura — banco, cache/sessão,
mensageria e identidade — sem nenhuma tabela de domínio (ver `specs/001-infraestrutura-base.md`,
seção 4). As primeiras migrations reais (`V1`–`V5`) entraram na spec 005 (domínio base:
Aluno, Curso, Disciplina, `curso_disciplina`, Turma). `V6`/`V7` entraram na spec 006
(Matrícula): tabela `matricula` e as colunas `vagas_ocupadas`/`version` em `turma`,
usadas pelo mecanismo de proteção de vaga sob concorrência (D024/D025). `V8` também é da
spec 006, mas não é dado de nenhum módulo de domínio — cria `event_publication`, a
tabela interna do Spring Modulith (`spring-modulith-events-jpa`) para o registro de
publicação de eventos de domínio (`@ApplicationModuleListener`, D029), rotulada
`infraestrutura` na convenção abaixo por ser cross-cutting.

## Convenção de nomenclatura (D015 em docs/DECISIONS.md)

Pasta única `db/migration` para todos os módulos Modulith — o schema Postgres é único
(`public`, D005), e a fronteira de módulo já é reforçada no código, não na estrutura de
arquivos. Cada migration segue:

```
V{sequencial}__{modulo}_{descricao_em_snake_case}.sql
```

- `{sequencial}`: inteiro crescente, único no repositório inteiro (não reinicia por
  módulo) — ex: `V1`, `V2`, `V3`, mesmo que `V2` seja do módulo `notificacao` e `V1`/`V3`
  do módulo `academico`. Evita a armadilha de múltiplas *locations* do Flyway, onde a
  ordem de aplicação depende só do número de versão, não da pasta de origem.
- `{modulo}`: nome do módulo Modulith dono da tabela (`academico`, `notificacao`, etc.),
  para dar contexto visual sem precisar abrir o arquivo.
- `{descricao_em_snake_case}`: o que a migration faz, em português, minúsculo, com `_`.

Exemplos: `V1__academico_criar_tabela_aluno.sql`, `V2__notificacao_criar_tabela_log_evento.sql`.
