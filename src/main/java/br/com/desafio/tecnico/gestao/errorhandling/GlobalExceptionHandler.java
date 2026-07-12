package br.com.desafio.tecnico.gestao.errorhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
		return ProblemDetailFactory.build(HttpStatus.CONFLICT, ex.getMessage(), ex.getErrorCode(),
				request.getRequestURI());
	}

	/**
	 * Rede de segurança (specs/006, seção 4.4): o consumo/liberação de vaga usa um
	 * UPDATE condicional atômico (TurmaRepository, D024), que nunca lança esta exceção
	 * diretamente (só devolve 0 linhas afetadas, tratado como ConflitoRegraNegocioException
	 * pelo MatriculaService). Este handler cobre qualquer outro ponto do código que
	 * usar save() direto em uma entidade com @Version e colidir. Mensagem genérica de
	 * propósito - não expõe nome da exceção, entidade ou número de versão ao cliente
	 * (atenção pedida explicitamente no security review desta fase).
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleConflitoConcorrencia(OptimisticLockingFailureException ex, HttpServletRequest request) {
		log.warn("Conflito de concorrência (lock otimista): metodo={} path={}", request.getMethod(),
				request.getRequestURI());
		return ProblemDetailFactory.build(HttpStatus.CONFLICT, "Conflito de concorrência, tente novamente.",
				"CONCURRENCY_CONFLICT", request.getRequestURI());
	}

	/**
	 * Rede de segurança (achado de code-reviewer/security-auditor da spec 006, D031):
	 * checagens de unicidade em código (existsBy...) são "checar depois agir" (TOCTOU)
	 * - sob duas requisições concorrentes (ex: duplo clique), ambas podem passar a
	 * checagem e uma delas violar a constraint UNIQUE/índice único parcial do banco
	 * (backstop de dupla garantia, D020/D023/D026). Sem este handler, isso vazava um
	 * 500 genérico em vez de um 409 de negócio. Mensagem propositalmente genérica -
	 * não expõe nome de constraint/tabela/coluna do banco ao cliente.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConflitoIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("Conflito de integridade de dados (constraint unica): metodo={} path={}", request.getMethod(),
				request.getRequestURI());
		return ProblemDetailFactory.build(HttpStatus.CONFLICT,
				"O recurso já existe ou viola uma regra de unicidade. Tente novamente.", "DATA_INTEGRITY_CONFLICT",
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
