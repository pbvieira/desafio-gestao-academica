package br.com.desafio.tecnico.gestao.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void erroGenericoNaoVazaMensagemOriginalNemStackTrace() {
		HttpServletRequest request = mockRequest("/api/qualquer");
		RuntimeException excecaoOriginal = new RuntimeException("detalhe interno sensivel: senha=1234");

		ProblemDetail problem = handler.handleErroGenerico(excecaoOriginal, request);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(problem.getDetail()).isEqualTo("Erro interno. Contate o suporte.");
		assertThat(problem.getDetail()).doesNotContain("senha=1234");
		assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_ERROR");
	}

	@Test
	void recursoNaoEncontradoVira404ComMensagemDaExcecao() {
		HttpServletRequest request = mockRequest("/api/exemplo/recurso/inexistente");

		ProblemDetail problem = handler
				.handleNaoEncontrado(new RecursoNaoEncontradoException("Recurso não encontrado."), request);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(problem.getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
	}

	@Test
	void conflitoRegraNegocioVira409() {
		HttpServletRequest request = mockRequest("/api/exemplo/recurso");

		ProblemDetail problem = handler.handleConflito(new ConflitoRegraNegocioException("Conflito."), request);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getProperties()).containsEntry("errorCode", "BUSINESS_CONFLICT");
	}

	@Test
	void conflitoRegraNegocioComErrorCodeExplicitoPreservaOCodigo() {
		HttpServletRequest request = mockRequest("/api/matriculas/1/confirmar");

		ProblemDetail problem = handler
				.handleConflito(new ConflitoRegraNegocioException("Vagas esgotadas.", "VAGAS_ESGOTADAS"), request);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getProperties()).containsEntry("errorCode", "VAGAS_ESGOTADAS");
	}

	@Test
	void conflitoDeConcorrenciaVira409SemVazarDetalheInterno() {
		HttpServletRequest request = mockRequest("/api/matriculas/1/confirmar");
		OptimisticLockingFailureException excecaoOriginal = new ObjectOptimisticLockingFailureException("Turma", 1L);

		ProblemDetail problem = handler.handleConflitoConcorrencia(excecaoOriginal, request);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getDetail()).isEqualTo("Conflito de concorrência, tente novamente.");
		assertThat(problem.getDetail()).doesNotContain("Turma").doesNotContain("ObjectOptimisticLockingFailureException");
		assertThat(problem.getProperties()).containsEntry("errorCode", "CONCURRENCY_CONFLICT");
	}

	@Test
	void conflitoDeIntegridadeVira409SemVazarDetalheDoBanco() {
		HttpServletRequest request = mockRequest("/api/matriculas");
		DataIntegrityViolationException excecaoOriginal = new DataIntegrityViolationException(
				"duplicate key value violates unique constraint \"uk_matricula_aluno_turma_ativa\"");

		ProblemDetail problem = handler.handleConflitoIntegridade(excecaoOriginal, request);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getDetail()).doesNotContain("uk_matricula_aluno_turma_ativa").doesNotContain("constraint");
		assertThat(problem.getProperties()).containsEntry("errorCode", "DATA_INTEGRITY_CONFLICT");
	}

	private HttpServletRequest mockRequest(String path) {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRequestURI()).thenReturn(path);
		Mockito.when(request.getMethod()).thenReturn("GET");
		return request;
	}

}
