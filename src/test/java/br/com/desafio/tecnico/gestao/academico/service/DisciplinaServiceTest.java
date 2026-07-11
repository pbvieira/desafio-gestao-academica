package br.com.desafio.tecnico.gestao.academico.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.dto.DisciplinaRequest;
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
class DisciplinaServiceTest {

	@Mock
	private DisciplinaRepository disciplinaRepository;

	@Mock
	private TurmaRepository turmaRepository;

	@InjectMocks
	private DisciplinaService disciplinaService;

	@Test
	void criar_comCodigoDuplicado_lancaConflito() {
		when(disciplinaRepository.existsByCodigo("MAT101")).thenReturn(true);

		assertThatThrownBy(
				() -> disciplinaService.criar(new DisciplinaRequest("MAT101", "Cálculo I")))
				.isInstanceOf(ConflitoRegraNegocioException.class);
		verify(disciplinaRepository, never()).save(any());
	}

	@Test
	void criar_comCodigoNovo_salvaDisciplina() {
		when(disciplinaRepository.existsByCodigo("MAT101")).thenReturn(false);
		when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Disciplina disciplina = disciplinaService.criar(new DisciplinaRequest("MAT101", "Cálculo I"));

		assertThat(disciplina.getCodigo()).isEqualTo("MAT101");
	}

	@Test
	void editar_comCodigoJaUsadoPorOutraDisciplina_lancaConflito() {
		Disciplina existente = new Disciplina();
		existente.setId(1L);
		existente.setCodigo("MAT101");
		when(disciplinaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
		when(disciplinaRepository.existsByCodigoAndIdNot("MAT102", 1L)).thenReturn(true);

		assertThatThrownBy(
				() -> disciplinaService.editar(1L, new DisciplinaRequest("MAT102", "Cálculo II")))
				.isInstanceOf(ConflitoRegraNegocioException.class);
	}

	@Test
	void buscarAtivo_idInexistente_lancaRecursoNaoEncontrado() {
		when(disciplinaRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> disciplinaService.buscarAtivo(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void excluir_comTurmaAtivaVinculada_lancaConflito() {
		Disciplina disciplina = new Disciplina();
		disciplina.setId(1L);
		disciplina.setCodigo("MAT101");
		when(disciplinaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByDisciplinaIdAndAtivoTrue(1L)).thenReturn(true);

		assertThatThrownBy(() -> disciplinaService.excluir(1L)).isInstanceOf(ConflitoRegraNegocioException.class);
		assertThat(disciplina.isAtivo()).isTrue();
	}

	@Test
	void excluir_semTurmaAtivaVinculada_inativaDisciplina() {
		Disciplina disciplina = new Disciplina();
		disciplina.setId(1L);
		when(disciplinaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByDisciplinaIdAndAtivoTrue(1L)).thenReturn(false);

		disciplinaService.excluir(1L);

		assertThat(disciplina.isAtivo()).isFalse();
	}

}
