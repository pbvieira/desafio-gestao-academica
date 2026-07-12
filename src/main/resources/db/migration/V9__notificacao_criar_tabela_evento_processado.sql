-- Idempotencia no consumo de eventos via RabbitMQ (specs/007-mensageria-rabbitmq.md,
-- D035): dedupe por eventId. Um redelivery da mesma mensagem (ex: falha de ack antes da
-- confirmacao ao broker) encontra o id ja presente e pula o reprocessamento, sem gerar
-- efeito duplicado (log duplicado hoje; notificacao real duplicada, se/quando existir).
CREATE TABLE evento_processado
(
    id            UUID PRIMARY KEY,
    tipo_evento   VARCHAR(64) NOT NULL,
    processado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
