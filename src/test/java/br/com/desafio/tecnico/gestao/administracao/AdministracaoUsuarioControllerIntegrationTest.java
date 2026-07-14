package br.com.desafio.tecnico.gestao.administracao;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * specs/010: cobre só a borda de autorização (@PreAuthorize de classe, ADMIN-only) -
 * mesmo padrão de AlunoControllerIntegrationTest. Não testa o corpo da resposta de
 * sucesso aqui (exigiria um Keycloak real respondendo à Admin API) - esse caminho é
 * validado manualmente (seção 10 da spec) contra a stack real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdministracaoUsuarioControllerIntegrationTest {

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
	void listarSemToken_retorna401() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void listarComPapelAluno_retorna403() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios").with(aluno())).andExpect(status().isForbidden());
	}

	@Test
	void listarComPapelSecretaria_retorna403() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios").with(secretaria())).andExpect(status().isForbidden());
	}

	private static RequestPostProcessor aluno() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUNO"));
	}

	private static RequestPostProcessor secretaria() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"));
	}

}
