package br.com.desafio.tecnico.gestao.academico;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de referência (specs/005, seção 4.3): criar → listar → editar → excluir contra um
 * Postgres real (Testcontainers), com migrations Flyway reais aplicadas - diferente do
 * Postgres de desenvolvimento do compose.yaml (D003), descartável por execução. O padrão
 * aqui serve de modelo para as outras três entidades (não replicado 1:1 para manter o
 * prazo - specs/005, seção 8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CursoIntegrationTest {

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
	void criarListarEditarExcluirCurso() throws Exception {
		String corpoCriacao = """
				{"codigo":"ADS","nome":"Análise e Desenvolvimento de Sistemas"}""";

		String resposta = mockMvc
				.perform(post("/api/cursos").with(secretaria())
						.contentType("application/json")
						.content(corpoCriacao))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("ADS"))
				.andExpect(jsonPath("$.ativo").value(true))
				.andReturn().getResponse().getContentAsString();

		Long id = com.jayway.jsonpath.JsonPath.read(resposta, "$.id") instanceof Integer i ? i.longValue()
				: (Long) com.jayway.jsonpath.JsonPath.read(resposta, "$.id");

		mockMvc.perform(get("/api/cursos").with(aluno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.codigo == 'ADS')]").exists());

		String corpoEdicao = """
				{"codigo":"ADS","nome":"Análise e Desenvolvimento de Sistemas (novo currículo)"}""";

		mockMvc.perform(put("/api/cursos/" + id).with(secretaria())
				.contentType("application/json")
				.content(corpoEdicao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Análise e Desenvolvimento de Sistemas (novo currículo)"));

		mockMvc.perform(delete("/api/cursos/" + id).with(secretaria()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/cursos/" + id).with(aluno()))
				.andExpect(status().isNotFound());
	}

	@Test
	void criarComCodigoDuplicado_retorna409() throws Exception {
		String corpo = """
				{"codigo":"ENG","nome":"Engenharia de Software"}""";

		mockMvc.perform(post("/api/cursos").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/cursos").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isConflict());
	}

	@Test
	void criarSemPermissaoDeEscrita_retorna403() throws Exception {
		String corpo = """
				{"codigo":"SIS","nome":"Sistemas de Informação"}""";

		mockMvc.perform(post("/api/cursos").with(aluno()).contentType("application/json").content(corpo))
				.andExpect(status().isForbidden());
	}

	@Test
	void criarComCodigoEmBranco_retorna400() throws Exception {
		String corpo = """
				{"codigo":"","nome":"Curso Sem Código"}""";

		mockMvc.perform(post("/api/cursos").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isBadRequest());
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor secretaria() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor aluno() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUNO"));
	}

}
