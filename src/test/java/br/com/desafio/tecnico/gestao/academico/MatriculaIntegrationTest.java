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
 * specs/006-matricula.md, seção 7: fluxo completo criar->confirmar->cancelar com
 * verificação de vagasOcupadas da Turma a cada passo, mais o primeiro caso real de ABAC
 * do projeto (aluno A não acessa matrícula de aluno B) - a spec 003 só tinha o exemplo
 * PERFIL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MatriculaIntegrationTest {

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
	void fluxoCompleto_criarConfirmarDuplicidadeCancelarRecriar() throws Exception {
		Long cursoId = criarCurso("MI-C1", "Curso Matrícula Integração 1");
		Long disciplinaId = criarDisciplina("MI-D1", "Disciplina Matrícula Integração 1");
		vincular(cursoId, disciplinaId);
		Long turmaId = criarTurma("MI-T1", cursoId, disciplinaId, 1);
		Long alunoId = criarAluno("Aluno MI", "aluno.mi@example.com", "sub-mi-1");

		Long matriculaId = criarMatricula(alunoId, turmaId, "sub-mi-1");
		assertVagasOcupadas(turmaId, 0);

		mockMvc.perform(post("/api/matriculas/" + matriculaId + "/confirmar").with(secretaria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"));
		assertVagasOcupadas(turmaId, 1);

		// confirmar de novo é idempotente (D028), não é erro
		mockMvc.perform(post("/api/matriculas/" + matriculaId + "/confirmar").with(secretaria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"));
		assertVagasOcupadas(turmaId, 1);

		// duplicidade: mesmo aluno, mesma turma, matrícula ainda ativa -> 409
		mockMvc.perform(post("/api/matriculas").with(aluno("sub-mi-1"))
				.contentType("application/json")
				.content(corpoMatricula(alunoId, turmaId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("MATRICULA_DUPLICADA"));

		mockMvc.perform(post("/api/matriculas/" + matriculaId + "/cancelar").with(aluno("sub-mi-1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADA"));
		assertVagasOcupadas(turmaId, 0);

		// D026: cancelada não bloqueia nova matrícula na mesma turma
		mockMvc.perform(post("/api/matriculas").with(aluno("sub-mi-1"))
				.contentType("application/json")
				.content(corpoMatricula(alunoId, turmaId)))
				.andExpect(status().isCreated());
	}

	@Test
	void vagasEsgotadas_segundaConfirmacaoNaUltimaVagaRetorna409() throws Exception {
		Long cursoId = criarCurso("MI-C2", "Curso Matrícula Integração 2");
		Long disciplinaId = criarDisciplina("MI-D2", "Disciplina Matrícula Integração 2");
		vincular(cursoId, disciplinaId);
		Long turmaId = criarTurma("MI-T2", cursoId, disciplinaId, 1);
		Long alunoAId = criarAluno("Aluno MI A2", "aluno.mi.a2@example.com", "sub-mi-a2");
		Long alunoBId = criarAluno("Aluno MI B2", "aluno.mi.b2@example.com", "sub-mi-b2");

		Long matriculaA = criarMatricula(alunoAId, turmaId, "sub-mi-a2");
		Long matriculaB = criarMatricula(alunoBId, turmaId, "sub-mi-b2");

		mockMvc.perform(post("/api/matriculas/" + matriculaA + "/confirmar").with(secretaria()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/matriculas/" + matriculaB + "/confirmar").with(secretaria()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("VAGAS_ESGOTADAS"));
	}

	@Test
	void abac_alunoNaoAcessaMatriculaDeOutroAluno() throws Exception {
		Long cursoId = criarCurso("MI-C3", "Curso Matrícula Integração 3");
		Long disciplinaId = criarDisciplina("MI-D3", "Disciplina Matrícula Integração 3");
		vincular(cursoId, disciplinaId);
		Long turmaId = criarTurma("MI-T3", cursoId, disciplinaId, 5);
		Long alunoAId = criarAluno("Aluno MI A3", "aluno.mi.a3@example.com", "sub-mi-a3");
		Long alunoBId = criarAluno("Aluno MI B3", "aluno.mi.b3@example.com", "sub-mi-b3");

		Long matriculaA = criarMatricula(alunoAId, turmaId, "sub-mi-a3");

		mockMvc.perform(get("/api/matriculas/" + matriculaA).with(aluno("sub-mi-b3")))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/matriculas/" + matriculaA + "/cancelar").with(aluno("sub-mi-b3")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/alunos/" + alunoAId + "/matriculas").with(aluno("sub-mi-b3")))
				.andExpect(status().isForbidden());

		// dono acessa normalmente
		mockMvc.perform(get("/api/matriculas/" + matriculaA).with(aluno("sub-mi-a3")))
				.andExpect(status().isOk());

		// staff acessa qualquer uma
		mockMvc.perform(get("/api/matriculas/" + matriculaA).with(secretaria()))
				.andExpect(status().isOk());

		// aluno tentando matricular outro aluno -> 403
		mockMvc.perform(post("/api/matriculas").with(aluno("sub-mi-b3"))
				.contentType("application/json")
				.content(corpoMatricula(alunoAId, turmaId)))
				.andExpect(status().isForbidden());
	}

	@Test
	void alunoNaoPodeConfirmarAPropriaMatricula() throws Exception {
		Long cursoId = criarCurso("MI-C4", "Curso Matrícula Integração 4");
		Long disciplinaId = criarDisciplina("MI-D4", "Disciplina Matrícula Integração 4");
		vincular(cursoId, disciplinaId);
		Long turmaId = criarTurma("MI-T4", cursoId, disciplinaId, 5);
		Long alunoId = criarAluno("Aluno MI 4", "aluno.mi4@example.com", "sub-mi-4");

		Long matriculaId = criarMatricula(alunoId, turmaId, "sub-mi-4");

		mockMvc.perform(post("/api/matriculas/" + matriculaId + "/confirmar").with(aluno("sub-mi-4")))
				.andExpect(status().isForbidden());
	}

	private Long criarCurso(String codigo, String nome) throws Exception {
		String resposta = mockMvc
				.perform(post("/api/cursos").with(secretaria()).contentType("application/json")
						.content("{\"codigo\":\"" + codigo + "\",\"nome\":\"" + nome + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarDisciplina(String codigo, String nome) throws Exception {
		String resposta = mockMvc
				.perform(post("/api/disciplinas").with(secretaria()).contentType("application/json")
						.content("{\"codigo\":\"" + codigo + "\",\"nome\":\"" + nome + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private void vincular(Long cursoId, Long disciplinaId) throws Exception {
		mockMvc.perform(post("/api/cursos/" + cursoId + "/disciplinas/" + disciplinaId).with(secretaria()))
				.andExpect(status().isOk());
	}

	private Long criarTurma(String codigo, Long cursoId, Long disciplinaId, int limiteVagas) throws Exception {
		String corpo = "{\"codigo\":\"" + codigo + "\",\"cursoId\":" + cursoId + ",\"disciplinaId\":" + disciplinaId
				+ ",\"limiteVagas\":" + limiteVagas + "}";
		String resposta = mockMvc
				.perform(post("/api/turmas").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarAluno(String nome, String email, String keycloakSubjectId) throws Exception {
		String corpo = "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"keycloakSubjectId\":\""
				+ keycloakSubjectId + "\"}";
		String resposta = mockMvc
				.perform(post("/api/alunos").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarMatricula(Long alunoId, Long turmaId, String subject) throws Exception {
		String resposta = mockMvc
				.perform(post("/api/matriculas").with(aluno(subject)).contentType("application/json")
						.content(corpoMatricula(alunoId, turmaId)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private String corpoMatricula(Long alunoId, Long turmaId) {
		return "{\"alunoId\":" + alunoId + ",\"turmaId\":" + turmaId + "}";
	}

	private void assertVagasOcupadas(Long turmaId, int esperado) throws Exception {
		mockMvc.perform(get("/api/turmas/" + turmaId).with(secretaria()))
				.andExpect(jsonPath("$.vagasOcupadas").value(esperado));
	}

	private Long idDe(String jsonBody) {
		Object id = com.jayway.jsonpath.JsonPath.read(jsonBody, "$.id");
		return id instanceof Integer i ? i.longValue() : (Long) id;
	}

	private static RequestPostProcessor secretaria() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"));
	}

	private static RequestPostProcessor aluno(String subject) {
		return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_ALUNO"));
	}

}
