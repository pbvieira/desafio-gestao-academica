Nenhuma migration ainda. A Fase 1 (`specs/001-infraestrutura-base.md`) cobre só a
infraestrutura — banco, cache/sessão, mensageria e identidade — e decidiu deliberadamente
não criar nenhuma tabela de domínio nem tabela auxiliar "de preenchimento" sem necessidade
concreta (ver seção 4 da spec). Este diretório existe apenas para o Flyway reconhecer a
location `classpath:db/migration` e criar a `flyway_schema_history` no schema `public`
(D005) já nesta fase — validado subindo a aplicação de fato.

A primeira migration real (`V1__...`) entra na Fase 2, junto da criação das entidades de
domínio (Aluno, Curso, Disciplina, Turma, Matrícula).

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
