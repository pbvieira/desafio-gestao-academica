CREATE TABLE curso (
    id         BIGSERIAL PRIMARY KEY,
    codigo     VARCHAR(20) NOT NULL,
    nome       VARCHAR(200) NOT NULL,
    ativo      BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_curso_codigo UNIQUE (codigo)
);
