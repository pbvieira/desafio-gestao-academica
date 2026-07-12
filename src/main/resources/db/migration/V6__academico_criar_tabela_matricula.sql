-- Indice unico parcial (nao UNIQUE simples): matricula CANCELADA nao bloqueia nova
-- matricula do mesmo aluno na mesma turma (D026 em docs/DECISIONS.md) - a checagem
-- equivalente tambem existe no service (dupla garantia, mesmo padrao de D020/D023).
CREATE TABLE matricula (
    id            BIGSERIAL PRIMARY KEY,
    aluno_id      BIGINT NOT NULL,
    turma_id      BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmado_em TIMESTAMPTZ,
    cancelado_em  TIMESTAMPTZ,
    CONSTRAINT fk_matricula_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (id),
    CONSTRAINT fk_matricula_turma FOREIGN KEY (turma_id) REFERENCES turma (id)
);

CREATE UNIQUE INDEX uk_matricula_aluno_turma_ativa ON matricula (aluno_id, turma_id)
    WHERE status <> 'CANCELADA';
