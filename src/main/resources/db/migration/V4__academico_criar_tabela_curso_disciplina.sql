-- Tabela de juncao Curso N:N Disciplina (D020 em docs/DECISIONS.md) - uma Disciplina
-- pode ser oferecida em mais de um Curso (ex: "Calculo I" comum a Engenharia e Fisica).
CREATE TABLE curso_disciplina (
    curso_id      BIGINT NOT NULL REFERENCES curso (id),
    disciplina_id BIGINT NOT NULL REFERENCES disciplina (id),
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_curso_disciplina PRIMARY KEY (curso_id, disciplina_id)
);
