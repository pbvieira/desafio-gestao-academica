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
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com", null);
		when(alunoRepository.existsByEmail("ana@example.com")).thenReturn(false);
		when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Aluno aluno = alunoService.criar(request);

		assertThat(aluno.getNome()).isEqualTo("Ana Silva");
		assertThat(aluno.getEmail()).isEqualTo("ana@example.com");
	}

	@Test
	void criar_comEmailDuplicado_lancaConflito() {
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com", null);
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

		AlunoRequest request = new AlunoRequest("Ana Silva", "outro@example.com", null);

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

		AlunoRequest request = new AlunoRequest("Ana P. Silva", "ana.silva@example.com", null);
		Aluno atualizado = alunoService.editar(1L, request);

		assertThat(atualizado.getNome()).isEqualTo("Ana P. Silva");
		assertThat(atualizado.getEmail()).isEqualTo("ana.silva@example.com");
	}

	@Test
	void criar_comKeycloakSubjectIdJaVinculadoAOutroAluno_lancaConflito() {
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com", "sub-123");
		when(alunoRepository.existsByEmail("ana@example.com")).thenReturn(false);
		when(alunoRepository.existsByKeycloakSubjectId("sub-123")).thenReturn(true);

		assertThatThrownBy(() -> alunoService.criar(request)).isInstanceOf(ConflitoRegraNegocioException.class);
		verify(alunoRepository, never()).save(any());
	}

	@Test
	void criar_comKeycloakSubjectIdDisponivel_vinculaAluno() {
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com", "sub-123");
		when(alunoRepository.existsByEmail("ana@example.com")).thenReturn(false);
		when(alunoRepository.existsByKeycloakSubjectId("sub-123")).thenReturn(false);
		when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Aluno aluno = alunoService.criar(request);

		assertThat(aluno.getKeycloakSubjectId()).isEqualTo("sub-123");
	}

	@Test
	void criar_comKeycloakSubjectIdEmBranco_tratadoComoSemVinculo() {
		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com", "   ");
		when(alunoRepository.existsByEmail("ana@example.com")).thenReturn(false);
		when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Aluno aluno = alunoService.criar(request);

		assertThat(aluno.getKeycloakSubjectId()).isNull();
		verify(alunoRepository, never()).existsByKeycloakSubjectId(any());
	}

	@Test
	void editar_comKeycloakSubjectIdJaVinculadoAOutroAluno_lancaConflito() {
		Aluno existente = new Aluno();
		existente.setId(1L);
		existente.setEmail("ana@example.com");
		when(alunoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
		when(alunoRepository.existsByEmailAndIdNot("ana@example.com", 1L)).thenReturn(false);
		when(alunoRepository.existsByKeycloakSubjectIdAndIdNot("sub-999", 1L)).thenReturn(true);

		AlunoRequest request = new AlunoRequest("Ana Silva", "ana@example.com", "sub-999");

		assertThatThrownBy(() -> alunoService.editar(1L, request))
				.isInstanceOf(ConflitoRegraNegocioException.class);
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

	@Test
	void buscarPorKeycloakSubjectId_vinculado_retornaAluno() {
		Aluno existente = new Aluno();
		existente.setId(1L);
		existente.setKeycloakSubjectId("sub-123");
		when(alunoRepository.findByKeycloakSubjectIdAndAtivoTrue("sub-123")).thenReturn(Optional.of(existente));

		Aluno aluno = alunoService.buscarPorKeycloakSubjectId("sub-123");

		assertThat(aluno.getId()).isEqualTo(1L);
	}

	@Test
	void buscarPorKeycloakSubjectId_semVinculo_lancaRecursoNaoEncontrado() {
		when(alunoRepository.findByKeycloakSubjectIdAndAtivoTrue("sub-desconhecido")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> alunoService.buscarPorKeycloakSubjectId("sub-desconhecido"))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
