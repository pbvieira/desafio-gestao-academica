-- FK composta para curso_disciplina (nao FKs separados para curso/disciplina): garante
-- no proprio banco que uma Turma so pode existir para um par (curso, disciplina) que ja
-- esta associado em curso_disciplina - reforca em DDL a regra de negocio equivalente
-- que o service tambem valida (D020 em docs/DECISIONS.md).
CREATE TABLE turma (
    id            BIGSERIAL PRIMARY KEY,
    codigo        VARCHAR(20) NOT NULL,
    curso_id      BIGINT NOT NULL,
    disciplina_id BIGINT NOT NULL,
    limite_vagas  INTEGER NOT NULL,
    status        VARCHAR(20) NOT NULL,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_turma_curso_disciplina FOREIGN KEY (curso_id, disciplina_id)
        REFERENCES curso_disciplina (curso_id, disciplina_id),
    CONSTRAINT uk_turma_curso_disciplina_codigo UNIQUE (curso_id, disciplina_id, codigo),
    CONSTRAINT ck_turma_limite_vagas_positivo CHECK (limite_vagas > 0)
);
