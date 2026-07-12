package br.com.desafio.tecnico.gestao.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import br.com.desafio.tecnico.gestao.academico.MatriculaCancelada;
import br.com.desafio.tecnico.gestao.academico.MatriculaConfirmada;
import br.com.desafio.tecnico.gestao.academico.MatriculaCriada;

/**
 * specs/006-matricula.md, seção 4.3: prova o desacoplamento entre `academico` e
 * `notificacao` via publicação/consumo de eventos (D029 em docs/DECISIONS.md - Spring
 * Modulith interno nesta fase, RabbitMQ só na Fase 4). Nenhuma lógica real de
 * notificação existe ainda - só o log estruturado abaixo, como pedido no escopo desta
 * fase.
 */
@Component
public class MatriculaNotificacaoListener {

	private static final Logger log = LoggerFactory.getLogger(MatriculaNotificacaoListener.class);

	@ApplicationModuleListener
	void aoCriar(MatriculaCriada evento) {
		log.info("Notificação seria enviada aqui - matrícula {} criada (aluno={}, turma={}).", evento.matriculaId(),
				evento.alunoId(), evento.turmaId());
	}

	@ApplicationModuleListener
	void aoConfirmar(MatriculaConfirmada evento) {
		log.info("Notificação seria enviada aqui - matrícula {} confirmada.", evento.matriculaId());
	}

	@ApplicationModuleListener
	void aoCancelar(MatriculaCancelada evento) {
		log.info("Notificação seria enviada aqui - matrícula {} cancelada (liberouVaga={}).", evento.matriculaId(),
				evento.liberouVaga());
	}

}
