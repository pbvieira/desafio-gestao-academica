A Fase 1 (`specs/001` a `specs/004`) cobriu só infraestrutura — banco, cache/sessão,
mensageria e identidade — sem nenhuma tabela de domínio (ver `specs/001-infraestrutura-base.md`,
seção 4). As primeiras migrations reais (`V1`–`V5`) entraram na spec 005 (domínio base:
Aluno, Curso, Disciplina, `curso_disciplina`, Turma) — Matrícula ainda não existe, fica
para a spec da Fase 3.

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
