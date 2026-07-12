package br.com.desafio.tecnico.gestao.academico.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.MatriculaCancelada;
import br.com.desafio.tecnico.gestao.academico.MatriculaConfirmada;
import br.com.desafio.tecnico.gestao.academico.MatriculaCriada;
import br.com.desafio.tecnico.gestao.academico.domain.Aluno;
import br.com.desafio.tecnico.gestao.academico.domain.Matricula;
import br.com.desafio.tecnico.gestao.academico.domain.StatusMatricula;
import br.com.desafio.tecnico.gestao.academico.domain.StatusTurma;
import br.com.desafio.tecnico.gestao.academico.domain.Turma;
import br.com.desafio.tecnico.gestao.academico.dto.MatriculaRequest;
import br.com.desafio.tecnico.gestao.academico.repository.AlunoRepository;
import br.com.desafio.tecnico.gestao.academico.repository.MatriculaRepository;
import br.com.desafio.tecnico.gestao.academico.repository.TurmaRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

/**
 * specs/006-matricula.md. Proteção de vaga via UPDATE condicional atômico em
 * TurmaRepository (D024/D025) - esta classe nunca lê vagasOcupadas/limiteVagas em Java
 * para decidir se confirma; a decisão é feita pelo próprio banco, atomicamente.
 */
@Service
public class MatriculaService {

	private final MatriculaRepository matriculaRepository;
	private final TurmaRepository turmaRepository;
	private final AlunoRepository alunoRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Tracer tracer;

	public MatriculaService(MatriculaRepository matriculaRepository, TurmaRepository turmaRepository,
			AlunoRepository alunoRepository, ApplicationEventPublisher eventPublisher, Tracer tracer) {
		this.matriculaRepository = matriculaRepository;
		this.turmaRepository = turmaRepository;
		this.alunoRepository = alunoRepository;
		this.eventPublisher = eventPublisher;
		this.tracer = tracer;
	}

	/**
	 * D034: precisa ser capturado AQUI - na thread da requisição HTTP original, antes de
	 * publishEvent() devolver o controle ao Modulith - porque a externalização para o
	 * RabbitMQ roda depois, de forma assíncrona numa thread separada sem o contexto de
	 * span desta requisição (achado empírico: um `MessagePostProcessor` no
	 * `RabbitTemplate`, que só roda no momento do publish assíncrono, sempre viu
	 * `tracer.currentSpan() == null`).
	 */
	private String traceIdAtual() {
		Span spanAtual = tracer.currentSpan();
		return spanAtual != null ? spanAtual.context().traceId() : null;
	}

	@Transactional
	public Matricula criar(MatriculaRequest request) {
		Aluno aluno = alunoRepository.findByIdAndAtivoTrue(request.alunoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Aluno com id '" + request.alunoId() + "' não encontrado."));
		Turma turma = turmaRepository.findByIdAndAtivoTrue(request.turmaId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Turma com id '" + request.turmaId() + "' não encontrada."));
		if (turma.getStatus() != StatusTurma.ABERTA) {
			throw new ConflitoRegraNegocioException("Turma '" + turma.getCodigo() + "' não está aberta.",
					"TURMA_FECHADA");
		}
		if (matriculaRepository.existsByAlunoIdAndTurmaIdAndStatusNot(aluno.getId(), turma.getId(),
				StatusMatricula.CANCELADA)) {
			throw new ConflitoRegraNegocioException(
					"Aluno '" + aluno.getId() + "' já está matriculado na turma '" + turma.getCodigo() + "'.",
					"MATRICULA_DUPLICADA");
		}
		Matricula matricula = new Matricula();
		matricula.setAluno(aluno);
		matricula.setTurma(turma);
		matricula = matriculaRepository.save(matricula);
		eventPublisher.publishEvent(new MatriculaCriada(UUID.randomUUID(), traceIdAtual(), matricula.getId(),
				aluno.getId(), turma.getId()));
		return matricula;
	}

	@Transactional
	public Matricula confirmar(Long id) {
		Matricula matricula = buscarPorId(id);
		if (matricula.getStatus() == StatusMatricula.CONFIRMADA) {
			// Idempotente (D028): confirmar de novo uma já confirmada não é erro.
			return matricula;
		}
		if (matricula.getStatus() == StatusMatricula.CANCELADA) {
			throw new ConflitoRegraNegocioException("Matrícula '" + id + "' está cancelada e não pode ser confirmada.",
					"TRANSICAO_INVALIDA");
		}
		Turma turma = matricula.getTurma();
		int linhasAfetadas = turmaRepository.consumirVaga(turma.getId(), turma.getVersion());
		if (linhasAfetadas == 0) {
			Turma turmaAtual = turmaRepository.findById(turma.getId())
					.orElseThrow(() -> new RecursoNaoEncontradoException("Turma '" + turma.getId() + "' não encontrada."));
			if (turmaAtual.getVagasOcupadas() >= turmaAtual.getLimiteVagas()) {
				throw new ConflitoRegraNegocioException(
						"Não há vagas disponíveis na turma '" + turmaAtual.getCodigo() + "'.", "VAGAS_ESGOTADAS");
			}
			throw new ConflitoRegraNegocioException(
					"Conflito de concorrência ao confirmar a matrícula '" + id + "'. Tente novamente.",
					"CONFLITO_CONCORRENCIA");
		}
		// Achado de code review (D031): NÃO usar matriculaRepository.save(matricula)
		// aqui. "matricula" ficou detached pelo clearAutomatically do consumirVaga
		// acima; save() faria merge(), e sem cascade=MERGE na associação, o merge cria
		// proxies NÃO inicializados para aluno/turma (reintroduzindo N+1) - e pior,
		// uma nova consulta com @EntityGraph não ajuda, porque o identity map já
		// devolveria essa mesma instância recém-mergeada em cache, sem re-executar a
		// query com o graph. Em vez disso, recarrega uma instância MANAGED (via
		// @EntityGraph) ANTES de mutar, e deixa o dirty checking do Hibernate gerar o
		// UPDATE no commit - sem merge(), aluno/turma continuam plenamente carregados.
		Matricula matriculaGerenciada = buscarPorId(id);
		matriculaGerenciada.setStatus(StatusMatricula.CONFIRMADA);
		matriculaGerenciada.setConfirmadoEm(Instant.now());
		eventPublisher.publishEvent(
				new MatriculaConfirmada(UUID.randomUUID(), traceIdAtual(), matriculaGerenciada.getId()));
		return matriculaGerenciada;
	}

	@Transactional
	public Matricula cancelar(Long id) {
		Matricula matricula = buscarPorId(id);
		if (matricula.getStatus() == StatusMatricula.CANCELADA) {
			throw new ConflitoRegraNegocioException("Matrícula '" + id + "' já está cancelada.", "TRANSICAO_INVALIDA");
		}
		boolean liberouVaga = false;
		if (matricula.getStatus() == StatusMatricula.CONFIRMADA) {
			liberarVagaComRetry(matricula.getTurma(), id);
			liberouVaga = true;
		}
		// Mesmo motivo de confirmar() (D031): recarrega MANAGED antes de mutar, sem
		// merge() via save() - dirty checking gera o UPDATE, aluno/turma continuam
		// plenamente carregados (sem N+1 ao montar a resposta).
		Matricula matriculaGerenciada = buscarPorId(id);
		matriculaGerenciada.setStatus(StatusMatricula.CANCELADA);
		matriculaGerenciada.setCanceladoEm(Instant.now());
		eventPublisher.publishEvent(new MatriculaCancelada(UUID.randomUUID(), traceIdAtual(),
				matriculaGerenciada.getId(), liberouVaga));
		return matriculaGerenciada;
	}

	/**
	 * Liberar vaga não disputa "quem libera primeiro" (ao contrário de consumir) -
	 * um único retry transparente após reconsultar a versão atual é suficiente para o
	 * caso raro de conflito por edição concorrente de outro campo da Turma
	 * (specs/006, seção 4.4; D028).
	 */
	private void liberarVagaComRetry(Turma turma, Long matriculaId) {
		int linhasAfetadas = turmaRepository.liberarVaga(turma.getId(), turma.getVersion());
		if (linhasAfetadas == 1) {
			return;
		}
		Turma turmaAtual = turmaRepository.findById(turma.getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Turma '" + turma.getId() + "' não encontrada."));
		linhasAfetadas = turmaRepository.liberarVaga(turmaAtual.getId(), turmaAtual.getVersion());
		if (linhasAfetadas == 0) {
			throw new ConflitoRegraNegocioException(
					"Conflito de concorrência ao cancelar a matrícula '" + matriculaId + "'. Tente novamente.",
					"CONFLITO_CONCORRENCIA");
		}
	}

	@Transactional(readOnly = true)
	public Matricula buscarPorId(Long id) {
		return matriculaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula com id '" + id + "' não encontrada."));
	}

	@Transactional(readOnly = true)
	public List<Matricula> listarPorAluno(Long alunoId) {
		return matriculaRepository.findByAlunoId(alunoId);
	}

	@Transactional(readOnly = true)
	public List<Matricula> listarPorTurma(Long turmaId) {
		return matriculaRepository.findByTurmaId(turmaId);
	}

}
