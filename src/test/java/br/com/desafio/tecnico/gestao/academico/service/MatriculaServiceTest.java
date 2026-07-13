package br.com.desafio.tecnico.gestao.academico.service;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

	@Mock
	private MatriculaRepository matriculaRepository;

	@Mock
	private TurmaRepository turmaRepository;

	@Mock
	private AlunoRepository alunoRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private Tracer tracer;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private MatriculaService matriculaService;

	@BeforeEach
	void setUp() {
		matriculaService = new MatriculaService(matriculaRepository, turmaRepository, alunoRepository,
				eventPublisher, tracer, meterRegistry);
	}

	@Test
	void criar_alunoInexistente_lancaRecursoNaoEncontrado() {
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.criar(new MatriculaRequest(1L, 10L)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void criar_turmaInexistente_lancaRecursoNaoEncontrado() {
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(aluno(1L)));
		when(turmaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.criar(new MatriculaRequest(1L, 10L)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void criar_turmaFechada_lancaConflito() {
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(aluno(1L)));
		when(turmaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.FECHADA, 30, 0)));

		assertThatThrownBy(() -> matriculaService.criar(new MatriculaRequest(1L, 10L)))
				.isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("TURMA_FECHADA");
	}

	@Test
	void criar_duplicidadeComMatriculaAtiva_lancaConflito() {
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(aluno(1L)));
		when(turmaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 0)));
		when(matriculaRepository.existsByAlunoIdAndTurmaIdAndStatusNot(1L, 10L, StatusMatricula.CANCELADA))
				.thenReturn(true);

		assertThatThrownBy(() -> matriculaService.criar(new MatriculaRequest(1L, 10L)))
				.isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("MATRICULA_DUPLICADA");
	}

	@Test
	void criar_semMatriculaAtivaExistente_salvaEPublicaEvento() {
		Aluno aluno = aluno(1L);
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 0);
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(aluno));
		when(turmaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(turma));
		when(matriculaRepository.existsByAlunoIdAndTurmaIdAndStatusNot(1L, 10L, StatusMatricula.CANCELADA))
				.thenReturn(false);
		when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> {
			Matricula m = invocation.getArgument(0);
			m.setId(100L);
			return m;
		});

		Matricula matricula = matriculaService.criar(new MatriculaRequest(1L, 10L));

		assertThat(matricula.getStatus()).isEqualTo(StatusMatricula.PENDENTE);
		ArgumentCaptor<MatriculaCriada> captor = ArgumentCaptor.forClass(MatriculaCriada.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().matriculaId()).isEqualTo(100L);
	}

	@Test
	void confirmar_matriculaInexistente_lancaRecursoNaoEncontrado() {
		when(matriculaRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.confirmar(1L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void confirmar_pendenteComVagaDisponivel_consomeVagaEPublicaEvento() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 29);
		Matricula matricula = matricula(1L, turma, StatusMatricula.PENDENTE);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.consumirVaga(10L, 0L)).thenReturn(1);

		Matricula resultado = matriculaService.confirmar(1L);

		assertThat(resultado.getStatus()).isEqualTo(StatusMatricula.CONFIRMADA);
		assertThat(resultado.getConfirmadoEm()).isNotNull();
		verify(eventPublisher).publishEvent(any(MatriculaConfirmada.class));
	}

	@Test
	void confirmar_semVagaDisponivel_lancaConflitoVagasEsgotadas() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 30);
		Matricula matricula = matricula(1L, turma, StatusMatricula.PENDENTE);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.consumirVaga(10L, 0L)).thenReturn(0);
		when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 30)));

		assertThatThrownBy(() -> matriculaService.confirmar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("VAGAS_ESGOTADAS");
		verify(matriculaRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(meterRegistry.counter("matricula.vaga.conflito", "motivo", "VAGAS_ESGOTADAS").count())
				.isEqualTo(1.0);
	}

	@Test
	void confirmar_conflitoDeVersaoSemVagasEsgotadas_lancaConflitoConcorrencia() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 20);
		Matricula matricula = matricula(1L, turma, StatusMatricula.PENDENTE);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.consumirVaga(10L, 0L)).thenReturn(0);
		// Reconsulta mostra que ainda há vaga -> a falha foi por conflito de versão, não vagas esgotadas.
		when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 21)));

		assertThatThrownBy(() -> matriculaService.confirmar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("CONFLITO_CONCORRENCIA");
		assertThat(meterRegistry.counter("matricula.vaga.conflito", "motivo", "CONFLITO_CONCORRENCIA").count())
				.isEqualTo(1.0);
	}

	@Test
	void confirmar_jaConfirmada_idempotenteNaoRepublicaEvento() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 25);
		Matricula matricula = matricula(1L, turma, StatusMatricula.CONFIRMADA);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

		Matricula resultado = matriculaService.confirmar(1L);

		assertThat(resultado.getStatus()).isEqualTo(StatusMatricula.CONFIRMADA);
		verify(turmaRepository, never()).consumirVaga(anyLong(), anyLong());
		verify(matriculaRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void confirmar_cancelada_lancaConflitoTransicaoInvalida() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 25);
		Matricula matricula = matricula(1L, turma, StatusMatricula.CANCELADA);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

		assertThatThrownBy(() -> matriculaService.confirmar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("TRANSICAO_INVALIDA");
	}

	@Test
	void cancelar_matriculaInexistente_lancaRecursoNaoEncontrado() {
		when(matriculaRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.cancelar(1L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void cancelar_pendente_naoMexeEmVagaEPublicaEvento() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 5);
		Matricula matricula = matricula(1L, turma, StatusMatricula.PENDENTE);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

		Matricula resultado = matriculaService.cancelar(1L);

		assertThat(resultado.getStatus()).isEqualTo(StatusMatricula.CANCELADA);
		verify(turmaRepository, never()).liberarVaga(anyLong(), anyLong());
		ArgumentCaptor<MatriculaCancelada> captor = ArgumentCaptor.forClass(MatriculaCancelada.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().liberouVaga()).isFalse();
	}

	@Test
	void cancelar_confirmada_liberaVagaEPublicaEvento() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 10);
		Matricula matricula = matricula(1L, turma, StatusMatricula.CONFIRMADA);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.liberarVaga(10L, 0L)).thenReturn(1);

		Matricula resultado = matriculaService.cancelar(1L);

		assertThat(resultado.getStatus()).isEqualTo(StatusMatricula.CANCELADA);
		ArgumentCaptor<MatriculaCancelada> captor = ArgumentCaptor.forClass(MatriculaCancelada.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().liberouVaga()).isTrue();
	}

	@Test
	void cancelar_confirmada_conflitoDeVersaoComRetryTransparente_sucesso() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 10);
		Matricula matricula = matricula(1L, turma, StatusMatricula.CONFIRMADA);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.liberarVaga(10L, 0L)).thenReturn(0);
		when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 10, 1L)));
		when(turmaRepository.liberarVaga(10L, 1L)).thenReturn(1);

		Matricula resultado = matriculaService.cancelar(1L);

		assertThat(resultado.getStatus()).isEqualTo(StatusMatricula.CANCELADA);
		verify(turmaRepository, times(2)).liberarVaga(eq(10L), anyLong());
	}

	@Test
	void cancelar_confirmada_conflitoDeVersaoPersistenteAposRetry_lancaConflito() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 10);
		Matricula matricula = matricula(1L, turma, StatusMatricula.CONFIRMADA);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.liberarVaga(10L, 0L)).thenReturn(0);
		when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 10, 1L)));
		when(turmaRepository.liberarVaga(10L, 1L)).thenReturn(0);

		assertThatThrownBy(() -> matriculaService.cancelar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("CONFLITO_CONCORRENCIA");
		verify(matriculaRepository, never()).save(any());
	}

	@Test
	void cancelar_jaCancelada_lancaConflitoTransicaoInvalida() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 5);
		Matricula matricula = matricula(1L, turma, StatusMatricula.CANCELADA);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

		assertThatThrownBy(() -> matriculaService.cancelar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("TRANSICAO_INVALIDA");
	}

	@Test
	void buscarPorId_inexistente_lancaRecursoNaoEncontrado() {
		when(matriculaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.buscarPorId(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listarPorAluno_delegaParaRepositorio() {
		Matricula matricula = matricula(1L, turma(10L, StatusTurma.ABERTA, 30, 0), StatusMatricula.PENDENTE);
		when(matriculaRepository.findByAlunoId(1L)).thenReturn(List.of(matricula));

		assertThat(matriculaService.listarPorAluno(1L)).containsExactly(matricula);
	}

	@Test
	void listarPorTurma_delegaParaRepositorio() {
		Matricula matricula = matricula(1L, turma(10L, StatusTurma.ABERTA, 30, 0), StatusMatricula.PENDENTE);
		when(matriculaRepository.findByTurmaId(10L)).thenReturn(List.of(matricula));

		assertThat(matriculaService.listarPorTurma(10L)).containsExactly(matricula);
	}

	private Aluno aluno(Long id) {
		Aluno aluno = new Aluno();
		aluno.setId(id);
		aluno.setNome("Aluno " + id);
		return aluno;
	}

	private Turma turma(Long id, StatusTurma status, int limiteVagas, int vagasOcupadas) {
		return turma(id, status, limiteVagas, vagasOcupadas, 0L);
	}

	private Turma turma(Long id, StatusTurma status, int limiteVagas, int vagasOcupadas, long version) {
		Turma turma = new Turma();
		turma.setId(id);
		turma.setCodigo("T" + id);
		turma.setStatus(status);
		turma.setLimiteVagas(limiteVagas);
		turma.setVagasOcupadas(vagasOcupadas);
		turma.setVersion(version);
		return turma;
	}

	private Matricula matricula(Long id, Turma turma, StatusMatricula status) {
		Matricula matricula = new Matricula();
		matricula.setId(id);
		matricula.setAluno(aluno(1L));
		matricula.setTurma(turma);
		matricula.setStatus(status);
		return matricula;
	}

}
