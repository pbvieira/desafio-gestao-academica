package br.com.desafio.tecnico.gestao.security.config;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import br.com.desafio.tecnico.gestao.errorhandling.ProblemDetailFactory;

/**
 * Diferencia 401 (não autenticado) de 403 (autenticado, sem permissão) - specs/003,
 * seção 3. O AuthenticationEntryPoint (401) é o único produtor possível desse status: a
 * autenticação falha antes de a requisição alcançar o DispatcherServlet, então nenhum
 * @RestControllerAdvice consegue tratá-lo (também reaproveitado pela cadeia de Basic
 * Auth do scrape do Prometheus, spec 002/004). O AccessDeniedHandler (403) só é um
 * fallback defensivo - na prática, negações de @PreAuthorize em métodos de controller
 * são interceptadas antes pelo GlobalExceptionHandler (specs/004), que é quem produz o
 * 403 "de verdade" na maioria dos casos. Ambos usam ProblemDetailFactory (D016),
 * garantindo um formato idêntico ao resto da API. Cada negação também vira uma linha de
 * log estruturado (specs/003, seção 8) - sem incluir o token/credencial em si.
 */
@Configuration
public class SecurityErrorHandlingConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityErrorHandlingConfig.class);

	@Bean
	AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
		return (request, response, authException) -> {
			log.warn("Acesso nao autenticado rejeitado: metodo={} path={} motivo={}", request.getMethod(),
					request.getRequestURI(), authException.getMessage());
			ProblemDetail problem = ProblemDetailFactory.build(HttpStatus.UNAUTHORIZED,
					"Autenticação necessária, ou token ausente/inválido/expirado.", "NOT_AUTHENTICATED",
					request.getRequestURI());
			writeProblem(response, objectMapper, HttpStatus.UNAUTHORIZED, problem);
		};
	}

	@Bean
	AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
		return (request, response, accessDeniedException) -> {
			log.warn("Acesso negado por falta de permissao (fallback fora do dispatch do MVC): metodo={} path={}",
					request.getMethod(), request.getRequestURI());
			ProblemDetail problem = ProblemDetailFactory.accessDenied(request.getRequestURI());
			writeProblem(response, objectMapper, HttpStatus.FORBIDDEN, problem);
		};
	}

	private void writeProblem(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
			ProblemDetail problem) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		// response.getWriter() usaria o encoding default da resposta (nem sempre UTF-8);
		// escrever direto no OutputStream faz o Jackson usar UTF-8, preservando acentos.
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

}
