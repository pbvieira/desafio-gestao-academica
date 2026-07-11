package br.com.desafio.tecnico.gestao.errorhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento padronizado de erros (specs/004): 400/404/409/500 e o 403 efetivo de
 * negações de @PreAuthorize em métodos de controller - este @RestControllerAdvice, por
 * ser um HandlerExceptionResolver do Spring MVC, intercepta a AccessDeniedException
 * ANTES dela alcançar o ExceptionTranslationFilter do Spring Security; o
 * AccessDeniedHandler registrado em SecurityConfig (specs/003) vira só um fallback
 * defensivo para negações fora do dispatch do MVC. 401 continua sendo produzido pelo
 * AuthenticationEntryPoint (specs/003) - a autenticação falha antes de a requisição
 * alcançar o DispatcherServlet, então @RestControllerAdvice não consegue tratá-lo.
 * Todos usam ProblemDetailFactory para um formato idêntico em toda a API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<Map<String, String>> erros = new ArrayList<>();
		ex.getBindingResult().getFieldErrors().forEach(fieldError -> erros
				.add(Map.of("campo", fieldError.getField(), "mensagem", String.valueOf(fieldError.getDefaultMessage()))));
		// getGlobalErrors() cobre validações de nível de classe (ex: @AssertTrue,
		// cross-field como "data fim >= data inicio") - achado de code review: ficavam
		// descartadas silenciosamente, devolvendo "erros": [] mesmo com falha real.
		ex.getBindingResult().getGlobalErrors().forEach(globalError -> erros.add(Map.of("campo",
				globalError.getObjectName(), "mensagem", String.valueOf(globalError.getDefaultMessage()))));
		ProblemDetail problem = ProblemDetailFactory.build(HttpStatus.BAD_REQUEST,
				"Erro de validação nos dados enviados.", "VALIDATION_ERROR", request.getRequestURI());
		problem.setProperty("erros", erros);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleCorpoMalformado(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return ProblemDetailFactory.build(HttpStatus.BAD_REQUEST, "Corpo da requisição malformado.",
				"VALIDATION_ERROR", request.getRequestURI());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
		return ProblemDetailFactory.build(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND",
				request.getRequestURI());
	}

	@ExceptionHandler(ConflitoRegraNegocioException.class)
	ProblemDetail handleConflito(ConflitoRegraNegocioException ex, HttpServletRequest request) {
		return ProblemDetailFactory.build(HttpStatus.CONFLICT, ex.getMessage(), "BUSINESS_CONFLICT",
				request.getRequestURI());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
		log.warn("Acesso negado por falta de permissao: metodo={} path={}", request.getMethod(),
				request.getRequestURI());
		return ProblemDetailFactory.accessDenied(request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleErroGenerico(Exception ex, HttpServletRequest request) {
		log.error("Erro interno nao tratado: metodo={} path={}", request.getMethod(), request.getRequestURI(), ex);
		return ProblemDetailFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Contate o suporte.",
				"INTERNAL_ERROR", request.getRequestURI());
	}

}
