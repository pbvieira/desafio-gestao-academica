-- Disciplina e' um item de catalogo compartilhavel entre Cursos (D020 em
-- docs/DECISIONS.md) - por isso o codigo e' unico globalmente, nao por curso.
CREATE TABLE disciplina (
    id         BIGSERIAL PRIMARY KEY,
    codigo     VARCHAR(20) NOT NULL,
    nome       VARCHAR(200) NOT NULL,
    ativo      BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_disciplina_codigo UNIQUE (codigo)
);
