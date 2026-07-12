package br.com.desafio.tecnico.gestao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do RabbitMQ para eventos de domínio (specs/007-mensageria-rabbitmq.md,
 * D032/D033/D035). Fica em `config` (cross-cutting, mesmo padrão de OpenApiConfig) - é
 * contrato compartilhado entre o publicador (`academico`, via @Externalized) e o
 * consumidor (`notificacao`, via @RabbitListener), não pertence a nenhum dos dois.
 *
 * <p>Topologia (D032/D033): exchange topic {@code gestao.eventos} → fila
 * {@code notificacao.matricula} (bound com {@code matricula.#}) → em falha (nack sem
 * requeue, ver application.properties), dead-letter para {@code notificacao.matricula.retry}
 * → após TTL fixo de 10s, dead-letter de volta para {@code notificacao.matricula}. Depois
 * de N tentativas (contadas pelo consumidor via header {@code x-death}), a mensagem é
 * publicada explicitamente em {@code notificacao.matricula.dlq} pelo próprio consumidor -
 * não há limite nativo de tentativas em fila clássica sem plugin extra, por isso essa
 * decisão fica na aplicação (ver MatriculaNotificacaoListener).
 */
@Configuration
public class MensageriaConfig {

	public static final String EXCHANGE_EVENTOS = "gestao.eventos";
	public static final String ROUTING_KEY_MATRICULA_TODAS = "matricula.#";
	public static final String ROUTING_KEY_MATRICULA_CRIADA = "matricula.criada";
	public static final String ROUTING_KEY_MATRICULA_CONFIRMADA = "matricula.confirmada";
	public static final String ROUTING_KEY_MATRICULA_CANCELADA = "matricula.cancelada";
	public static final String FILA_MATRICULA = "notificacao.matricula";
	public static final String FILA_MATRICULA_RETRY = "notificacao.matricula.retry";
	public static final String FILA_MATRICULA_DLQ = "notificacao.matricula.dlq";

	/**
	 * Externalizado via property (D033: 10s em produção) para os testes de integração
	 * poderem sobrescrever com um valor curto (ex: 1-2s) e não ficarem lentos
	 * esperando o TTL real - specs/007, seção 4.6.
	 */
	@Value("${mensageria.retry.ttl-ms:10000}")
	private int retryTtlMs;

	@Bean
	TopicExchange gestaoEventosExchange() {
		return new TopicExchange(EXCHANGE_EVENTOS, true, false);
	}

	@Bean
	Queue matriculaQueue() {
		return QueueBuilder.durable(FILA_MATRICULA)
				.withArgument("x-dead-letter-exchange", "")
				.withArgument("x-dead-letter-routing-key", FILA_MATRICULA_RETRY)
				.build();
	}

	@Bean
	Binding matriculaBinding(Queue matriculaQueue, TopicExchange gestaoEventosExchange) {
		return BindingBuilder.bind(matriculaQueue).to(gestaoEventosExchange).with(ROUTING_KEY_MATRICULA_TODAS);
	}

	/**
	 * Fila de espera, não consumida diretamente (sem binding a nenhum exchange) - só
	 * "estaciona" a mensagem pelo TTL antes de devolvê-la à fila principal.
	 */
	@Bean
	Queue matriculaRetryQueue() {
		return QueueBuilder.durable(FILA_MATRICULA_RETRY)
				.withArgument("x-message-ttl", retryTtlMs)
				.withArgument("x-dead-letter-exchange", "")
				.withArgument("x-dead-letter-routing-key", FILA_MATRICULA)
				.build();
	}

	/**
	 * Destino final - escrita explicitamente pelo consumidor após esgotar as tentativas
	 * (não por dead-letter automático), ver MatriculaNotificacaoListener.
	 */
	@Bean
	Queue matriculaDlq() {
		return QueueBuilder.durable(FILA_MATRICULA_DLQ).build();
	}

}
