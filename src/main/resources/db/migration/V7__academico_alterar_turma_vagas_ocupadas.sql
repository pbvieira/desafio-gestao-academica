-- vagas_ocupadas: campo incremental (nao COUNT calculado), atualizado por UPDATE
-- condicional atomico no mesmo statement que checa o limite (D024/D025 em
-- docs/DECISIONS.md) - a atomicidade dessa checagem+escrita e' o que protege a ultima
-- vaga sob concorrencia. version: lock otimista (@Version do JPA) para o restante dos
-- campos da Turma.
ALTER TABLE turma
    ADD COLUMN vagas_ocupadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE turma
    ADD CONSTRAINT ck_turma_vagas_ocupadas_valida
        CHECK (vagas_ocupadas >= 0 AND vagas_ocupadas <= limite_vagas);
