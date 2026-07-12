package br.com.desafio.tecnico.gestao.academico;

import java.util.UUID;

import org.springframework.modulith.events.Externalized;

import br.com.desafio.tecnico.gestao.config.MensageriaConfig;

/**
 * specs/006-matricula.md, seção 4.3 - não republicado na confirmação idempotente
 * (CONFIRMADA->CONFIRMADA, D028), só na transição real PENDENTE->CONFIRMADA.
 * specs/007-mensageria-rabbitmq.md, seção 4.2: externalizado para RabbitMQ (D032/D035).
 * `traceId` (D034): ver javadoc de MatriculaCriada - mesmo motivo (capturado na thread da
 * requisição HTTP original, não no momento da publicação assíncrona).
 */
@Externalized(MensageriaConfig.EXCHANGE_EVENTOS + "::" + MensageriaConfig.ROUTING_KEY_MATRICULA_CONFIRMADA)
public record MatriculaConfirmada(UUID eventId, String traceId, Long matriculaId) {
}
