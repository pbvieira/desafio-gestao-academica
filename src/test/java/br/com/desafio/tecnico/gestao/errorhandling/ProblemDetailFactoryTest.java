package br.com.desafio.tecnico.gestao.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

	@Test
	void montaProblemDetailComTypeErrorCodeTimestampEPath() {
		ProblemDetail problem = ProblemDetailFactory.build(HttpStatus.BAD_REQUEST, "detalhe", "VALIDATION_ERROR",
				"/api/exemplo");

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(problem.getDetail()).isEqualTo("detalhe");
		assertThat(problem.getType().toString()).isEqualTo("urn:gestao:erro:validation-error");
		assertThat(problem.getProperties()).containsEntry("errorCode", "VALIDATION_ERROR");
		assertThat(problem.getProperties()).containsEntry("path", "/api/exemplo");
		assertThat(problem.getProperties()).containsKey("timestamp");
	}

}
