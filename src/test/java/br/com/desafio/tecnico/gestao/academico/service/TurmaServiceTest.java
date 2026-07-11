package br.com.desafio.tecnico.gestao.academico.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.tecnico.gestao.academico.domain.Curso;
import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.domain.Turma;
import br.com.desafio.tecnico.gestao.academico.dto.TurmaRequest;
import br.com.desafio.tecnico.gestao.academico.repository.CursoRepository;
import br.com.desafio.tecnico.gestao.academico.repository.DisciplinaRepository;
import br.com.desafio.tecnico.gestao.academico.repository.TurmaRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

	@Mock
	private TurmaRepository turmaRepository;

	@Mock
	private CursoRepository cursoRepository;

	@Mock
	private DisciplinaRepository disciplinaRepository;

	@InjectMocks
	private TurmaService turmaService;

	@Test
	void criar_comCursoInexistente_lancaRecursoNaoEncontrado() {
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.criar(new TurmaRequest("T1", 1L, 10L, 30)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void criar_comDisciplinaInexistente_lancaRecursoNaoEncontrado() {
		Curso curso = cursoAtivo(1L, "ADS");
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.criar(new TurmaRequest("T1", 1L, 10L, 30)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void criar_comParCursoDisciplinaNaoAssociado_lancaConflito() {
		Curso curso = cursoAtivo(1L, "ADS");
		Disciplina disciplina = disciplinaAtiva(10L, "MAT101");
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(disciplina));

		assertThatThrownBy(() -> turmaService.criar(new TurmaRequest("T1", 1L, 10L, 30)))
				.isInstanceOf(ConflitoRegraNegocioException.class);
		verify(turmaRepository, never()).save(any());
	}

	@Test
	void criar_comCodigoDuplicadoParaMesmoParCursoDisciplina_lancaConflito() {
		Curso curso = cursoAtivo(1L, "ADS");
		Disciplina disciplina = disciplinaAtiva(10L, "MAT101");
		curso.getDisciplinas().add(disciplina);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByCursoIdAndDisciplinaIdAndCodigo(1L, 10L, "T1")).thenReturn(true);

		assertThatThrownBy(() -> turmaService.criar(new TurmaRequest("T1", 1L, 10L, 30)))
				.isInstanceOf(ConflitoRegraNegocioException.class);
	}

	@Test
	void criar_comParAssociadoECodigoNovo_salvaTurma() {
		Curso curso = cursoAtivo(1L, "ADS");
		Disciplina disciplina = disciplinaAtiva(10L, "MAT101");
		curso.getDisciplinas().add(disciplina);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByCursoIdAndDisciplinaIdAndCodigo(1L, 10L, "T1")).thenReturn(false);
		when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Turma turma = turmaService.criar(new TurmaRequest("T1", 1L, 10L, 30));

		assertThat(turma.getCodigo()).isEqualTo("T1");
		assertThat(turma.getLimiteVagas()).isEqualTo(30);
		assertThat(turma.getCurso()).isEqualTo(curso);
		assertThat(turma.getDisciplina()).isEqualTo(disciplina);
	}

	@Test
	void editar_comParAssociadoECodigoLivre_atualizaTurma() {
		Curso curso = cursoAtivo(1L, "ADS");
		Disciplina disciplina = disciplinaAtiva(10L, "MAT101");
		curso.getDisciplinas().add(disciplina);
		Turma existente = new Turma();
		existente.setId(5L);
		when(turmaRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(existente));
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByCursoIdAndDisciplinaIdAndCodigoAndIdNot(1L, 10L, "T2", 5L)).thenReturn(false);

		Turma atualizada = turmaService.editar(5L, new TurmaRequest("T2", 1L, 10L, 40));

		assertThat(atualizada.getCodigo()).isEqualTo("T2");
		assertThat(atualizada.getLimiteVagas()).isEqualTo(40);
	}

	@Test
	void buscarAtiva_idInexistente_lancaRecursoNaoEncontrado() {
		when(turmaRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.buscarAtiva(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void excluir_inativaTurmaSemRemoverRegistro() {
		Turma existente = new Turma();
		existente.setId(5L);
		when(turmaRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(existente));

		turmaService.excluir(5L);

		assertThat(existente.isAtivo()).isFalse();
		verify(turmaRepository, never()).delete(any());
	}

	private Curso cursoAtivo(Long id, String codigo) {
		Curso curso = new Curso();
		curso.setId(id);
		curso.setCodigo(codigo);
		return curso;
	}

	private Disciplina disciplinaAtiva(Long id, String codigo) {
		Disciplina disciplina = new Disciplina();
		disciplina.setId(id);
		disciplina.setCodigo(codigo);
		return disciplina;
	}

}
