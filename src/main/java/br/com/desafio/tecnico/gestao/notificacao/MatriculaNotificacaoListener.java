package br.com.desafio.tecnico.gestao.notificacao;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.tecnico.gestao.academico.MatriculaCancelada;
import br.com.desafio.tecnico.gestao.academico.MatriculaConfirmada;
import br.com.desafio.tecnico.gestao.academico.MatriculaCriada;
import br.com.desafio.tecnico.gestao.config.MensageriaConfig;
import br.com.desafio.tecnico.gestao.notificacao.domain.EventoProcessado;
import br.com.desafio.tecnico.gestao.notificacao.repository.EventoProcessadoRepository;

/**
 * specs/007-mensageria-rabbitmq.md, seção 4.4: consumidor real via RabbitMQ (não mais
 * @ApplicationModuleListener interno da Fase 3) - prova o desacoplamento
 * academico/notificacao através do broker de verdade, com retry+DLQ (D033) e
 * idempotência (D035). Nenhuma lógica real de notificação existe ainda - só o log
 * estruturado, como pedido no escopo da Fase 3/4.
 */
@Component
public class MatriculaNotificacaoListener {

	private static final Logger log = LoggerFactory.getLogger(MatriculaNotificacaoListener.class);
	private static final int MAX_TENTATIVAS = 3;
	private static final String MOTIVO_REJEITADO = "rejected";

	private final ObjectMapper objectMapper;
	private final EventoProcessadoRepository eventoProcessadoRepository;
	private final RabbitTemplate rabbitTemplate;
	private final MeterRegistry meterRegistry;

	public MatriculaNotificacaoListener(ObjectMapper objectMapper,
			EventoProcessadoRepository eventoProcessadoRepository, RabbitTemplate rabbitTemplate,
			MeterRegistry meterRegistry) {
		this.objectMapper = objectMapper;
		this.eventoProcessadoRepository = eventoProcessadoRepository;
		this.rabbitTemplate = rabbitTemplate;
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Uma única fila (D035) recebe os 3 tipos de evento (routing key `matricula.#`) -
	 * a routing key recebida decide como desserializar. Em falha, deixa a exceção
	 * propagar (nack sem requeue, application.properties) para o RabbitMQ rotear via
	 * dead-letter-exchange da fila (config.MensageriaConfig) - exceto na última
	 * tentativa permitida, quando o próprio consumidor publica na DLQ explicitamente
	 * (não há limite nativo de tentativas em fila clássica sem plugin extra).
	 */
	@RabbitListener(queues = MensageriaConfig.FILA_MATRICULA)
	@Transactional
	public void processar(Message mensagem) throws IOException {
		String traceId = traceIdDoPayload(mensagem);
		if (traceId != null) {
			MDC.put("traceId", traceId);
		}
		try {
			String routingKey = routingKeyOriginal(mensagem);
			try {
				switch (routingKey) {
					case MensageriaConfig.ROUTING_KEY_MATRICULA_CRIADA -> processarCriada(mensagem);
					case MensageriaConfig.ROUTING_KEY_MATRICULA_CONFIRMADA -> processarConfirmada(mensagem);
					case MensageriaConfig.ROUTING_KEY_MATRICULA_CANCELADA -> processarCancelada(mensagem);
					default -> log.warn("Routing key desconhecida, mensagem ignorada: {}", routingKey);
				}
			} catch (Exception ex) {
				if (esgotouTentativas(mensagem)) {
					enviarParaDlq(mensagem, ex);
					return;
				}
				throw ex;
			}
		} finally {
			if (traceId != null) {
				MDC.remove("traceId");
			}
		}
	}

	/**
	 * D034 (docs/DECISIONS.md): nem a config nativa de Observation
	 * (spring.rabbitmq.template/listener.observation-enabled) nem um
	 * `MessagePostProcessor` no `RabbitTemplate` propagam o traceId pelo caminho de
	 * externalização do Modulith - verificado empiricamente que, em ambos os casos,
	 * `tracer.currentSpan()` já está `null` no momento do publish, porque a
	 * externalização roda numa thread assíncrona separada (ex: "task-1") sem o contexto
	 * de span da requisição HTTP original. Fallback real: o traceId é capturado na
	 * própria thread da requisição (MatriculaService) e viaja como campo do payload do
	 * evento - lido aqui via árvore JSON genérica (sem amarrar a um tipo de evento
	 * específico) antes de saber qual dos 3 tipos a mensagem representa.
	 */
	private String traceIdDoPayload(Message mensagem) {
		try {
			JsonNode raiz = objectMapper.readTree(mensagem.getBody());
			JsonNode valor = raiz.get("traceId");
			return valor != null && !valor.isNull() ? valor.asText() : null;
		} catch (IOException ex) {
			return null;
		}
	}

	private void processarCriada(Message mensagem) throws IOException {
		MatriculaCriada evento = objectMapper.readValue(mensagem.getBody(), MatriculaCriada.class);
		if (jaProcessado(evento.eventId(), "MATRICULA_CRIADA")) {
			return;
		}
		log.info("Notificação seria enviada aqui - matrícula {} criada (aluno={}, turma={}).", evento.matriculaId(),
				evento.alunoId(), evento.turmaId());
	}

	private void processarConfirmada(Message mensagem) throws IOException {
		MatriculaConfirmada evento = objectMapper.readValue(mensagem.getBody(), MatriculaConfirmada.class);
		if (jaProcessado(evento.eventId(), "MATRICULA_CONFIRMADA")) {
			return;
		}
		log.info("Notificação seria enviada aqui - matrícula {} confirmada.", evento.matriculaId());
	}

	private void processarCancelada(Message mensagem) throws IOException {
		MatriculaCancelada evento = objectMapper.readValue(mensagem.getBody(), MatriculaCancelada.class);
		if (jaProcessado(evento.eventId(), "MATRICULA_CANCELADA")) {
			return;
		}
		log.info("Notificação seria enviada aqui - matrícula {} cancelada (liberouVaga={}).", evento.matriculaId(),
				evento.liberouVaga());
	}

	/**
	 * A PK de `evento_processado` é o próprio mecanismo de dedupe (D035) - o
	 * existsById evita a maioria dos redeliveries sem round-trip de erro, mas sob uma
	 * corrida rara (ex: reenvio manual via Management UI enquanto a entrega original
	 * ainda está em voo) o INSERT pode violar a PK; tratado como "já processado", não
	 * como falha nova (senão contaria indevidamente para o limite de tentativas).
	 */
	private boolean jaProcessado(UUID eventId, String tipoEvento) {
		if (eventoProcessadoRepository.existsById(eventId)) {
			log.info("Evento {} ({}) já processado - ignorando redelivery (idempotência).", eventId, tipoEvento);
			return true;
		}
		EventoProcessado registro = new EventoProcessado();
		registro.setId(eventId);
		registro.setTipoEvento(tipoEvento);
		try {
			eventoProcessadoRepository.saveAndFlush(registro);
		} catch (DataIntegrityViolationException ex) {
			log.info("Evento {} ({}) já processado (corrida na checagem de idempotência) - ignorando.", eventId,
					tipoEvento);
			return true;
		}
		return false;
	}

	/**
	 * Achado ao rodar o teste de integração (não era óbvio por leitura de código):
	 * `getReceivedRoutingKey()` reflete a routing key do ÚLTIMO hop de entrega, não a
	 * original. Após um bounce fila-principal → fila-de-retry → fila-principal
	 * (dead-letter via exchange padrão, D033), a partir da 2ª tentativa essa routing
	 * key "recebida" vira o NOME DA FILA de destino do dead-letter
	 * ("notificacao.matricula"), não mais "matricula.criada"/etc - sem este ajuste, a
	 * 2ª tentativa em diante caía sempre no branch `default` (routing key
	 * "desconhecida"), a mensagem era silenciosamente descartada (ack implícito, sem
	 * exceção) e nunca chegava na DLQ. A entrada do header `x-death` com
	 * fila=notificacao.matricula/motivo=rejected preserva a routing key ORIGINAL (com
	 * que a mensagem chegou na fila principal da primeira vez) em `routing-keys` -
	 * usada aqui como fonte de verdade quando presente.
	 */
	private String routingKeyOriginal(Message mensagem) {
		Map<String, Object> morte = mortePorFilaEMotivo(mensagem, MensageriaConfig.FILA_MATRICULA, MOTIVO_REJEITADO);
		if (morte != null && morte.get("routing-keys") instanceof List<?> chaves && !chaves.isEmpty()) {
			return String.valueOf(chaves.get(0));
		}
		return mensagem.getMessageProperties().getReceivedRoutingKey();
	}

	private boolean esgotouTentativas(Message mensagem) {
		return contarTentativasAnteriores(mensagem) + 1 >= MAX_TENTATIVAS;
	}

	private long contarTentativasAnteriores(Message mensagem) {
		Map<String, Object> morte = mortePorFilaEMotivo(mensagem, MensageriaConfig.FILA_MATRICULA, MOTIVO_REJEITADO);
		if (morte == null) {
			return 0;
		}
		Object count = morte.get("count");
		return count instanceof Number numero ? numero.longValue() : 0;
	}

	/**
	 * O RabbitMQ acrescenta/incrementa uma entrada no header `x-death` a cada
	 * dead-letter, agrupada por (fila, motivo) - a entrada com fila=notificacao.matricula
	 * e motivo=rejected é a única que representa falhas de PROCESSAMENTO (motivo=expired
	 * é o bounce da fila de retry pelo TTL, não uma falha em si).
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> mortePorFilaEMotivo(Message mensagem, String fila, String motivo) {
		Object xDeath = mensagem.getMessageProperties().getHeaders().get("x-death");
		if (!(xDeath instanceof List<?> mortes)) {
			return null;
		}
		for (Object item : mortes) {
			if (item instanceof Map<?, ?> entrada) {
				boolean filaBate = fila.equals(String.valueOf(entrada.get("queue")));
				boolean motivoBate = motivo.equals(String.valueOf(entrada.get("reason")));
				if (filaBate && motivoBate) {
					return (Map<String, Object>) entrada;
				}
			}
		}
		return null;
	}

	private void enviarParaDlq(Message mensagemOriginal, Exception causa) {
		rabbitTemplate.send("", MensageriaConfig.FILA_MATRICULA_DLQ, mensagemOriginal);
		log.warn("Mensagem esgotou {} tentativas e foi enviada para a DLQ ({}): {}", MAX_TENTATIVAS,
				MensageriaConfig.FILA_MATRICULA_DLQ, causa.getMessage());
		meterRegistry.counter("mensageria.dlq.eventos").increment();
	}

}
