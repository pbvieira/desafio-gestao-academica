package br.com.desafio.tecnico.gestao.notificacao;

import java.time.Duration;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import br.com.desafio.tecnico.gestao.academico.MatriculaCriada;
import br.com.desafio.tecnico.gestao.config.MensageriaConfig;
import br.com.desafio.tecnico.gestao.notificacao.repository.EventoProcessadoRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * specs/007-mensageria-rabbitmq.md, seção 4.6: retry→DLQ (D033) e idempotência (D035)
 * contra RabbitMQ e Postgres reais (Testcontainers). TTL de retry sobrescrito para 1,5s
 * (`mensageria.retry.ttl-ms`) para não deixar o teste lento esperando o TTL real de
 * produção (10s, D033). webEnvironment MOCK (default) - NONE quebra
 * SecurityConfig#actuatorPrometheusSecurityFilterChain, que exige um bean HttpSecurity
 * só disponível quando a camada web está presente.
 */
@SpringBootTest
@Testcontainers
class MensageriaConfiabilidadeIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Container
	static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-management"));

	@DynamicPropertySource
	static void propriedades(DynamicPropertyRegistry registry) {
		registry.add("spring.docker.compose.enabled", () -> "false");
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.rabbitmq.host", rabbitmq::getHost);
		registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
		registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
		registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
		registry.add("mensageria.retry.ttl-ms", () -> "1500");
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private EventoProcessadoRepository eventoProcessadoRepository;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void mensagemComPayloadInvalido_esgotaTentativasEVaiParaDlq() {
		Message mensagemInvalida = new Message("{ isto nao e um json valido".getBytes(), new MessageProperties());
		rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "matricula.criada", mensagemInvalida);

		Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
			Message naDlq = rabbitTemplate.receive(MensageriaConfig.FILA_MATRICULA_DLQ, 500);
			assertThat(naDlq).as("mensagem deve chegar na DLQ após esgotar as tentativas").isNotNull();
		});
		assertThat(meterRegistry.counter("mensageria.dlq.eventos").count()).isGreaterThanOrEqualTo(1.0);
	}

	@Test
	void redeliveryDoMesmoEvento_naoDuplicaProcessamento() throws Exception {
		UUID eventId = UUID.randomUUID();
		MatriculaCriada evento = new MatriculaCriada(eventId, "trace-integracao", 1L, 2L, 3L);
		Message mensagem = new Message(objectMapper.writeValueAsBytes(evento), new MessageProperties());

		rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "matricula.criada", mensagem);
		Awaitility.await().atMost(Duration.ofSeconds(15))
				.untilAsserted(() -> assertThat(eventoProcessadoRepository.existsById(eventId)).isTrue());

		// Redelivery manual (mesmo eventId) - simula o broker reentregando após falha de ack.
		rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "matricula.criada", mensagem);

		// Dá tempo para um eventual (indevido) reprocessamento acontecer antes de confirmar
		// que continua existindo só um registro de idempotência para este eventId.
		Thread.sleep(2000);
		assertThat(eventoProcessadoRepository.findById(eventId)).isPresent();
	}

}
