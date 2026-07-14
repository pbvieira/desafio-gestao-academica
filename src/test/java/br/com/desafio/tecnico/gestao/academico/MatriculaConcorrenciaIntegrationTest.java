package br.com.desafio.tecnico.gestao.academico;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * specs/006-matricula.md, seção 3/7: prova o critério eliminatório do PRD §06 - duas
 * requisições disputando a última vaga de uma Turma, exatamente uma confirma. Cada
 * chamada roda em sua própria thread/transação (MockMvc contra um SpringBootTest
 * completo com Testcontainers - nunca @Transactional na classe de teste, senão as duas
 * "requisições concorrentes" na verdade compartilhariam a mesma transação e o cenário
 * de corrida nunca aconteceria de verdade). @RepeatedTest roda o cenário várias vezes
 * na mesma execução para descartar flakiness - uma race condition que "às vezes passa"
 * não é prova de nada (pedido explícito do usuário para esta fase).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MatriculaConcorrenciaIntegrationTest {

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

	@RepeatedTest(10)
	void duasRequisicoesConcorrentesDisputandoUltimaVaga_apenasUmaConfirma() throws Exception {
		String subjectA = "sub-conc-a-" + System.nanoTime();
		String subjectB = "sub-conc-b-" + System.nanoTime();
		Long cursoId = criarCurso();
		Long disciplinaId = criarDisciplina();
		vincular(cursoId, disciplinaId);
		Long turmaId = criarTurma(cursoId, disciplinaId, 1);
		Long alunoAId = criarAluno(subjectA);
		Long alunoBId = criarAluno(subjectB);
		Long matriculaA = criarMatricula(alunoAId, turmaId, subjectA);
		Long matriculaB = criarMatricula(alunoBId, turmaId, subjectB);

		CyclicBarrier barreira = new CyclicBarrier(2);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Callable<Integer> confirmarA = () -> {
				barreira.await();
				return confirmarStatus(matriculaA);
			};
			Callable<Integer> confirmarB = () -> {
				barreira.await();
				return confirmarStatus(matriculaB);
			};

			List<Future<Integer>> futures = executor.invokeAll(List.of(confirmarA, confirmarB));
			List<Integer> statusCodes = new ArrayList<>();
			for (Future<Integer> future : futures) {
				statusCodes.add(future.get());
			}

			long sucessos = statusCodes.stream().filter(codigo -> codigo == 200).count();
			long conflitos = statusCodes.stream().filter(codigo -> codigo == 409).count();

			assertThat(sucessos).as("exatamente uma confirmação deve ter sucesso").isEqualTo(1);
			assertThat(conflitos).as("exatamente uma confirmação deve receber conflito").isEqualTo(1);
			mockMvc.perform(get("/api/turmas/" + turmaId).with(secretaria()))
					.andExpect(jsonPath("$.vagasOcupadas").value(1));
		} finally {
			executor.shutdown();
		}
	}

	private int confirmarStatus(Long matriculaId) throws Exception {
		return mockMvc.perform(post("/api/matriculas/" + matriculaId + "/confirmar").with(secretaria()))
				.andReturn().getResponse().getStatus();
	}

	private Long criarCurso() throws Exception {
		String codigo = "CONC-C-" + sufixoUnico();
		String resposta = mockMvc
				.perform(post("/api/cursos").with(secretaria()).contentType("application/json")
						.content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Curso Concorrência\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarDisciplina() throws Exception {
		String codigo = "CONC-D-" + sufixoUnico();
		String resposta = mockMvc
				.perform(post("/api/disciplinas").with(secretaria()).contentType("application/json")
						.content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Disciplina Concorrência\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private void vincular(Long cursoId, Long disciplinaId) throws Exception {
		mockMvc.perform(post("/api/cursos/" + cursoId + "/disciplinas/" + disciplinaId).with(secretaria()))
				.andExpect(status().isOk());
	}

	private Long criarTurma(Long cursoId, Long disciplinaId, int limiteVagas) throws Exception {
		String codigo = "CONC-T-" + sufixoUnico();
		String corpo = "{\"codigo\":\"" + codigo + "\",\"cursoId\":" + cursoId + ",\"disciplinaId\":" + disciplinaId
				+ ",\"limiteVagas\":" + limiteVagas + "}";
		String resposta = mockMvc
				.perform(post("/api/turmas").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarAluno(String keycloakSubjectId) throws Exception {
		String email = keycloakSubjectId + "@example.com";
		String corpo = "{\"nome\":\"Aluno " + keycloakSubjectId + "\",\"email\":\"" + email
				+ "\",\"keycloakSubjectId\":\"" + keycloakSubjectId + "\"}";
		String resposta = mockMvc
				.perform(post("/api/alunos").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarMatricula(Long alunoId, Long turmaId, String subject) throws Exception {
		String resposta = mockMvc
				.perform(post("/api/matriculas").with(aluno(subject)).contentType("application/json")
						.content("{\"alunoId\":" + alunoId + ",\"turmaId\":" + turmaId + "}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	/**
	 * "CONC-C-"/"CONC-D-"/"CONC-T-" (7 caracteres) + este sufixo precisa caber no
	 * limite de 20 caracteres do campo código (CursoRequest/DisciplinaRequest/
	 * TurmaRequest) - System.nanoTime() puro (até 19 dígitos) estourava esse limite e
	 * causava 400 em vez de 201 (achado ao rodar o teste pela primeira vez).
	 */
	private String sufixoUnico() {
		return String.valueOf(System.nanoTime() % 1_000_000_000L);
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
