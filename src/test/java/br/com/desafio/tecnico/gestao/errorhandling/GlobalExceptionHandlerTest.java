package br.com.desafio.tecnico.gestao.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

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

	private HttpServletRequest mockRequest(String path) {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRequestURI()).thenReturn(path);
		Mockito.when(request.getMethod()).thenReturn("GET");
		return request;
	}

}
