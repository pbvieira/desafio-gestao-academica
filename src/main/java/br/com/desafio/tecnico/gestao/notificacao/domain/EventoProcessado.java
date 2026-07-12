package br.com.desafio.tecnico.gestao.notificacao.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * specs/007-mensageria-rabbitmq.md, seção 4.4 (D035): dedupe de idempotência no
 * consumo de eventos via RabbitMQ. A chave primária (id = eventId do evento de
 * domínio) é o próprio mecanismo de dedupe - um INSERT duplicado viola a PK antes de
 * qualquer checagem em código ter chance de correr uma condição de corrida real.
 */
@Entity
@Table(name = "evento_processado")
@Getter
@Setter
@NoArgsConstructor
public class EventoProcessado {

	@Id
	private UUID id;

	@Column(name = "tipo_evento", nullable = false, length = 64)
	private String tipoEvento;

	@Column(name = "processado_em", nullable = false)
	private Instant processadoEm = Instant.now();

}
