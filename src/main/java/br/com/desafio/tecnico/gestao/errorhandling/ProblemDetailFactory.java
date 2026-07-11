package br.com.desafio.tecnico.gestao.errorhandling;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Ponto único de montagem do corpo de erro padronizado (RFC 7807/ProblemDetail) da API
 * (specs/004, D016) - usado tanto pelo GlobalExceptionHandler (400/404/409/500 e 403 de
 * @PreAuthorize em métodos de controller) quanto pelos handlers 401/403 de nível de
 * filtro do Spring Security (specs/003), garantindo um formato idêntico em toda a API.
 * "type" é uma URN estável por categoria de erro (não uma URL real); "errorCode" é uma
 * cópia curta pensada para o frontend comparar sem parsear a URN.
 */
public final class ProblemDetailFactory {

	private ProblemDetailFactory() {
	}

	public static ProblemDetail build(HttpStatus status, String detail, String errorCode, String path) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("urn:gestao:erro:" + errorCode.toLowerCase().replace('_', '-')));
		problem.setProperty("errorCode", errorCode);
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("path", path);
		return problem;
	}

	/**
	 * Caso comum a dois pontos do código (GlobalExceptionHandler e o AccessDeniedHandler
	 * de fallback em SecurityErrorHandlingConfig, specs/003) - achado de code review:
	 * evita a mesma mensagem/errorCode duplicados como string literal em dois arquivos.
	 */
	public static ProblemDetail accessDenied(String path) {
		return build(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.", "ACCESS_DENIED",
				path);
	}

}
