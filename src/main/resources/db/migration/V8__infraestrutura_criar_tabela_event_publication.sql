-- Tabela interna do Spring Modulith (spring-modulith-events-jpa, ja no pom.xml desde a
-- Fase 1) - registro de publicacao de eventos de dominio (@ApplicationModuleListener),
-- usada pelo framework para garantir entrega mesmo apos falha/reinicio da aplicacao.
-- Nao e' dado de nenhum modulo especifico (academico/notificacao) - e' infraestrutura
-- cross-cutting que passa a ser exercitada de verdade na spec 006 (Matricula, D029),
-- primeira vez que o projeto publica eventos de dominio. Schema copiado literalmente
-- de org.springframework.modulith:spring-modulith-events-jdbc:1.4.12
-- (schema-postgresql.sql) - a mesma estrutura que spring-modulith-events-jpa espera via
-- mapeamento JPA (DefaultJpaEventPublication), so' que aqui gerenciada via Flyway em vez
-- de auto-inicializacao do Modulith, para manter uma unica fonte de verdade de schema
-- (D014/D015 em docs/DECISIONS.md).
CREATE TABLE event_publication
(
    id               UUID NOT NULL,
    listener_id      TEXT NOT NULL,
    event_type       TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date  TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
