package br.com.desafio.tecnico.gestao.notificacao;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.desafio.tecnico.gestao.academico.MatriculaCancelada;
import br.com.desafio.tecnico.gestao.academico.MatriculaConfirmada;
import br.com.desafio.tecnico.gestao.academico.MatriculaCriada;
import br.com.desafio.tecnico.gestao.config.MensageriaConfig;
import br.com.desafio.tecnico.gestao.notificacao.domain.EventoProcessado;
import br.com.desafio.tecnico.gestao.notificacao.repository.EventoProcessadoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaNotificacaoListenerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private EventoProcessadoRepository eventoProcessadoRepository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private MatriculaNotificacaoListener listener;

	@BeforeEach
	void setUp() {
		listener = new MatriculaNotificacaoListener(objectMapper, eventoProcessadoRepository, rabbitTemplate,
				meterRegistry);
	}

	@Test
	void processarCriada_eventoNovo_gravaIdempotenciaSemErro() throws Exception {
		UUID eventId = UUID.randomUUID();
		when(eventoProcessadoRepository.existsById(eventId)).thenReturn(false);
		Message mensagem = mensagem("matricula.criada", new MatriculaCriada(eventId, "trace-abc", 1L, 2L, 3L));

		listener.processar(mensagem);

		ArgumentCaptor<EventoProcessado> captor = ArgumentCaptor.forClass(EventoProcessado.class);
		verify(eventoProcessadoRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(eventId);
		assertThat(captor.getValue().getTipoEvento()).isEqualTo("MATRICULA_CRIADA");
	}

	@Test
	void processarConfirmada_eventoJaProcessado_naoRegravaNemFalha() throws Exception {
		UUID eventId = UUID.randomUUID();
		when(eventoProcessadoRepository.existsById(eventId)).thenReturn(true);
		Message mensagem = mensagem("matricula.confirmada", new MatriculaConfirmada(eventId, "trace-abc", 1L));

		listener.processar(mensagem);

		verify(eventoProcessadoRepository, never()).saveAndFlush(any());
	}

	@Test
	void processarCancelada_corridaNaIdempotencia_tratadaComoJaProcessado() throws Exception {
		UUID eventId = UUID.randomUUID();
		when(eventoProcessadoRepository.existsById(eventId)).thenReturn(false);
		when(eventoProcessadoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
		Message mensagem = mensagem("matricula.cancelada", new MatriculaCancelada(eventId, "trace-abc", 1L, true));

		listener.processar(mensagem);

		verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
	}

	/**
	 * D034: prova a propagação real do traceId - capturado na thread da requisição
	 * (viaja no payload do evento, não num header AMQP) e colocado no MDC durante o
	 * processamento, removido ao final independente do resultado.
	 */
	@Test
	void processarCriada_comTraceId_colocaTraceIdNoMdcDuranteOProcessamentoERemoveDepois() throws Exception {
		UUID eventId = UUID.randomUUID();
		AtomicReference<String> traceIdCapturadoDuranteOProcessamento = new AtomicReference<>();
		when(eventoProcessadoRepository.existsById(eventId)).thenAnswer(invocation -> {
			traceIdCapturadoDuranteOProcessamento.set(MDC.get("traceId"));
			return false;
		});
		Message mensagem = mensagem("matricula.criada", new MatriculaCriada(eventId, "trace-xyz", 1L, 2L, 3L));

		listener.processar(mensagem);

		assertThat(traceIdCapturadoDuranteOProcessamento.get()).isEqualTo("trace-xyz");
		assertThat(MDC.get("traceId")).isNull();
	}

	@Test
	void routingKeyDesconhecida_naoLancaExcecao() throws Exception {
		Message mensagem = new Message("{}".getBytes(), propriedades("evento.desconhecido"));

		listener.processar(mensagem);
	}

	@Test
	void falhaDeProcessamento_primeiraTentativa_relancaExcecaoSemIrParaDlq() throws Exception {
		UUID eventId = UUID.randomUUID();
		when(eventoProcessadoRepository.existsById(eventId)).thenThrow(new RuntimeException("falha simulada"));
		Message mensagem = mensagem("matricula.criada", new MatriculaCriada(eventId, "trace-abc", 1L, 2L, 3L));

		assertThatThrownBy(() -> listener.processar(mensagem)).isInstanceOf(RuntimeException.class)
				.hasMessage("falha simulada");
		verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
	}

	@Test
	void falhaDeProcessamento_ultimaTentativaPermitida_vaiParaDlqSemRelancar() throws Exception {
		UUID eventId = UUID.randomUUID();
		when(eventoProcessadoRepository.existsById(eventId)).thenThrow(new RuntimeException("falha simulada"));
		Message mensagem = mensagemComTentativasAnteriores("matricula.criada", new MatriculaCriada(eventId, "trace-abc", 1L, 2L, 3L),
				2);

		listener.processar(mensagem);

		verify(rabbitTemplate).send(eq(""), eq(MensageriaConfig.FILA_MATRICULA_DLQ), eq(mensagem));
		assertThat(meterRegistry.counter("mensageria.dlq.eventos").count()).isEqualTo(1.0);
	}

	private Message mensagem(String routingKey, Object payload) throws Exception {
		return new Message(objectMapper.writeValueAsBytes(payload), propriedades(routingKey));
	}

	private Message mensagemComTentativasAnteriores(String routingKey, Object payload, int tentativasAnteriores)
			throws Exception {
		MessageProperties propriedades = propriedades(routingKey);
		propriedades.getHeaders().put("x-death",
				List.of(Map.of("queue", MensageriaConfig.FILA_MATRICULA, "reason", "rejected", "count",
						(long) tentativasAnteriores)));
		return new Message(objectMapper.writeValueAsBytes(payload), propriedades);
	}

	private MessageProperties propriedades(String routingKey) {
		MessageProperties propriedades = new MessageProperties();
		propriedades.setReceivedRoutingKey(routingKey);
		return propriedades;
	}

}
