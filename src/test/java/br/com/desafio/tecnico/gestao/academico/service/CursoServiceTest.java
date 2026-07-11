package br.com.desafio.tecnico.gestao.academico.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.tecnico.gestao.academico.domain.Curso;
import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.dto.CursoRequest;
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
class CursoServiceTest {

	@Mock
	private CursoRepository cursoRepository;

	@Mock
	private DisciplinaRepository disciplinaRepository;

	@Mock
	private TurmaRepository turmaRepository;

	@InjectMocks
	private CursoService cursoService;

	@Test
	void criar_comCodigoDuplicado_lancaConflito() {
		when(cursoRepository.existsByCodigo("ADS")).thenReturn(true);

		assertThatThrownBy(() -> cursoService.criar(new CursoRequest("ADS", "Análise e Desenvolvimento")))
				.isInstanceOf(ConflitoRegraNegocioException.class);
		verify(cursoRepository, never()).save(any());
	}

	@Test
	void criar_comCodigoNovo_salvaCurso() {
		when(cursoRepository.existsByCodigo("ADS")).thenReturn(false);
		when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Curso curso = cursoService.criar(new CursoRequest("ADS", "Análise e Desenvolvimento"));

		assertThat(curso.getCodigo()).isEqualTo("ADS");
	}

	@Test
	void editar_comCodigoJaUsadoPorOutroCurso_lancaConflito() {
		Curso existente = new Curso();
		existente.setId(1L);
		existente.setCodigo("ADS");
		existente.setNome("Análise e Desenvolvimento");
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
		when(cursoRepository.existsByCodigoAndIdNot("ENG", 1L)).thenReturn(true);

		assertThatThrownBy(() -> cursoService.editar(1L, new CursoRequest("ENG", "Engenharia")))
				.isInstanceOf(ConflitoRegraNegocioException.class);
	}

	@Test
	void buscarAtivo_idInexistente_lancaRecursoNaoEncontrado() {
		when(cursoRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cursoService.buscarAtivo(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void excluir_comDisciplinaAtivaVinculada_lancaConflito() {
		Curso curso = new Curso();
		curso.setId(1L);
		curso.setCodigo("ADS");
		Disciplina disciplina = new Disciplina();
		disciplina.setId(10L);
		disciplina.setAtivo(true);
		curso.getDisciplinas().add(disciplina);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));

		assertThatThrownBy(() -> cursoService.excluir(1L)).isInstanceOf(ConflitoRegraNegocioException.class);
		assertThat(curso.isAtivo()).isTrue();
	}

	@Test
	void excluir_semDisciplinaAtivaVinculada_inativaCurso() {
		Curso curso = new Curso();
		curso.setId(1L);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));

		cursoService.excluir(1L);

		assertThat(curso.isAtivo()).isFalse();
	}

	@Test
	void vincularDisciplina_associaDisciplinaAtivaAoCurso() {
		Curso curso = new Curso();
		curso.setId(1L);
		Disciplina disciplina = new Disciplina();
		disciplina.setId(10L);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(disciplina));

		Curso atualizado = cursoService.vincularDisciplina(1L, 10L);

		assertThat(atualizado.getDisciplinas()).contains(disciplina);
	}

	@Test
	void vincularDisciplina_disciplinaInexistente_lancaRecursoNaoEncontrado() {
		Curso curso = new Curso();
		curso.setId(1L);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cursoService.vincularDisciplina(1L, 99L))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void desvincularDisciplina_removeAssociacaoExistente() {
		Curso curso = new Curso();
		curso.setId(1L);
		Disciplina disciplina = new Disciplina();
		disciplina.setId(10L);
		curso.getDisciplinas().add(disciplina);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));

		Curso atualizado = cursoService.desvincularDisciplina(1L, 10L);

		assertThat(atualizado.getDisciplinas()).isEmpty();
	}

	@Test
	void desvincularDisciplina_semAssociacaoExistente_lancaRecursoNaoEncontrado() {
		Curso curso = new Curso();
		curso.setId(1L);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));

		assertThatThrownBy(() -> cursoService.desvincularDisciplina(1L, 10L))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void desvincularDisciplina_comTurmaVinculadaAoPar_lancaConflito() {
		Curso curso = new Curso();
		curso.setId(1L);
		Disciplina disciplina = new Disciplina();
		disciplina.setId(10L);
		curso.getDisciplinas().add(disciplina);
		when(cursoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(curso));
		when(turmaRepository.existsByCursoIdAndDisciplinaId(1L, 10L)).thenReturn(true);

		assertThatThrownBy(() -> cursoService.desvincularDisciplina(1L, 10L))
				.isInstanceOf(ConflitoRegraNegocioException.class);
		assertThat(curso.getDisciplinas()).contains(disciplina);
	}

}
