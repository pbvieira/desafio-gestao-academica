package br.com.desafio.tecnico.gestao.academico;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * specs/008-frontend-angular.md, D041: GET /api/alunos/me sobrescreve o @PreAuthorize de
 * classe (staff-only) do AlunoController - exatamente o tipo de interação sutil que vale
 * a pena provar contra a cadeia real do Spring Security (não só a lógica de negócio em
 * AlunoServiceTest), achado de code review. Mesmo padrão de Testcontainers de
 * CursoIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AlunoControllerIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@Container
	static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-management"));

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.docker.compose.enabled", () -> "false");
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.rabbitmq.host", rabbitmq::getHost);
		registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
		registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
		registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void meuPerfilSemToken_retorna401() throws Exception {
		mockMvc.perform(get("/api/alunos/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void meuPerfilComAlunoSemVinculo_retorna404() throws Exception {
		mockMvc.perform(get("/api/alunos/me").with(aluno("subject-sem-vinculo")))
				.andExpect(status().isNotFound());
	}

	@Test
	void meuPerfilComAlunoVinculado_retorna200ComOProprioRegistro() throws Exception {
		String corpo = """
				{"nome":"Ana Silva","email":"ana.me@example.com","keycloakSubjectId":"subject-ana"}""";
		mockMvc.perform(post("/api/alunos").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/alunos/me").with(aluno("subject-ana")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Silva"))
				.andExpect(jsonPath("$.email").value("ana.me@example.com"));
	}

	@Test
	void meuPerfilNaoRetornaRegistroDeOutroAluno() throws Exception {
		String corpoAna = """
				{"nome":"Ana Silva","email":"ana2.me@example.com","keycloakSubjectId":"subject-ana-2"}""";
		String corpoBeto = """
				{"nome":"Beto Souza","email":"beto.me@example.com","keycloakSubjectId":"subject-beto"}""";
		mockMvc.perform(post("/api/alunos").with(secretaria()).contentType("application/json").content(corpoAna))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/alunos").with(secretaria()).contentType("application/json").content(corpoBeto))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/alunos/me").with(aluno("subject-beto")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Beto Souza"));
	}

	@Test
	void listarTodosOsAlunosContinuaStaffOnly_regressaoDoOverrideDeClasse() throws Exception {
		mockMvc.perform(get("/api/alunos").with(aluno("qualquer-subject")))
				.andExpect(status().isForbidden());
	}

	private static RequestPostProcessor secretaria() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"));
	}

	private static RequestPostProcessor aluno(String subject) {
		return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_ALUNO"));
	}

}
