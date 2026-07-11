package br.com.desafio.tecnico.gestao.academico.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.tecnico.gestao.academico.domain.Aluno;
import br.com.desafio.tecnico.gestao.academico.dto.AlunoRequest;
import br.com.desafio.tecnico.gestao.academico.repository.AlunoRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

	@Mock
	private AlunoRepository alunoRepository;

	@InjectMocks
	private AlunoService alunoService;

	@Test
	void criar_comEmailNovo_salvaAluno() {
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com");
		when(alunoRepository.existsByEmail("ana@example.com")).thenReturn(false);
		when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Aluno aluno = alunoService.criar(request);

		assertThat(aluno.getNome()).isEqualTo("Ana Silva");
		assertThat(aluno.getEmail()).isEqualTo("ana@example.com");
	}

	@Test
	void criar_comEmailDuplicado_lancaConflito() {
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com");
		when(alunoRepository.existsByEmail("ana@example.com")).thenReturn(true);

		assertThatThrownBy(() -> alunoService.criar(request)).isInstanceOf(ConflitoRegraNegocioException.class);
		verify(alunoRepository, never()).save(any());
	}

	@Test
	void editar_comEmailJaUsadoPorOutroAluno_lancaConflito() {
		Aluno existente = new Aluno();
		existente.setId(1L);
		existente.setNome("Ana Silva");
		existente.setEmail("ana@example.com");
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
		when(alunoRepository.existsByEmailAndIdNot("outro@example.com", 1L)).thenReturn(true);

		AlunoRequest request = new AlunoRequest("Ana Silva", "outro@example.com");

		assertThatThrownBy(() -> alunoService.editar(1L, request))
				.isInstanceOf(ConflitoRegraNegocioException.class);
	}

	@Test
	void editar_comDadosValidos_atualizaAluno() {
		Aluno existente = new Aluno();
		existente.setId(1L);
		existente.setNome("Ana Silva");
		existente.setEmail("ana@example.com");
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
		when(alunoRepository.existsByEmailAndIdNot(eq("ana.silva@example.com"), eq(1L))).thenReturn(false);

		AlunoRequest request = new AlunoRequest("Ana P. Silva", "ana.silva@example.com");
		Aluno atualizado = alunoService.editar(1L, request);

		assertThat(atualizado.getNome()).isEqualTo("Ana P. Silva");
		assertThat(atualizado.getEmail()).isEqualTo("ana.silva@example.com");
	}

	@Test
	void buscarAtivo_idInexistente_lancaRecursoNaoEncontrado() {
		when(alunoRepository.findByIdAndAtivoTrue(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> alunoService.buscarAtivo(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listar_delegaParaRepositorioDeAtivos() {
		Aluno aluno = new Aluno();
		aluno.setId(1L);
		when(alunoRepository.findByAtivoTrue()).thenReturn(List.of(aluno));

		assertThat(alunoService.listar()).containsExactly(aluno);
	}

	@Test
	void excluir_inativaAlunoSemRemoverRegistro() {
		Aluno existente = new Aluno();
		existente.setId(1L);
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));

		alunoService.excluir(1L);

		assertThat(existente.isAtivo()).isFalse();
		verify(alunoRepository, never()).delete(any());
	}

}
