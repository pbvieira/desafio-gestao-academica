CREATE TABLE aluno (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(200) NOT NULL,
    email               VARCHAR(200) NOT NULL,
    -- Executa D006 (docs/DECISIONS.md): mapeamento Keycloak<->Aluno adiado ate Aluno
    -- existir. Nullable porque nem todo Aluno cadastrado pela secretaria tera,
    -- necessariamente, uma conta Keycloak associada no momento do cadastro.
    keycloak_subject_id VARCHAR(64),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_aluno_email UNIQUE (email),
    CONSTRAINT uk_aluno_keycloak_subject_id UNIQUE (keycloak_subject_id)
);
