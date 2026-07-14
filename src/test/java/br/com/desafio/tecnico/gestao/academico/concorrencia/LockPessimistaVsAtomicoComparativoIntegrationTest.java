package br.com.desafio.tecnico.gestao.academico.concorrencia;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import br.com.desafio.tecnico.gestao.academico.repository.TurmaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * specs/012-concorrencia-e-testes.md, D051: compara numericamente a estratégia atômica
 * já em produção (D024, TurmaRepository.consumirVaga) com uma variante de lock
 * pessimista (ConfirmacaoPessimistaService, só neste pacote de teste), N=10 threads
 * disputando M=1 vaga, uma execução por estratégia - o rigor de 10 repetições fica na
 * prova e2e (e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts). A corrida
 * em si chama repository/service diretamente, não HTTP/MockMvc (só o setup de
 * curso/disciplina/turma usa MockMvc, reaproveitando o padrão de
 * MatriculaConcorrenciaIntegrationTest) - contraste deliberado com a prova e2e, que é a
 * nível de transporte HTTP real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LockPessimistaVsAtomicoComparativoIntegrationTest {

	private static final int N_THREADS = 10;

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

	@Autowired
	private TurmaRepository turmaRepository;

	@Autowired
	private ConfirmacaoPessimistaService confirmacaoPessimistaService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void comparaEstrategiaAtomicaComPessimista_ambasConfirmamExatamenteUmaDeDezThreads() throws Exception {
		ResultadoComparacao atomica = rodarEstrategia("ATOMICA (UPDATE condicional, D024)", this::confirmarAtomico);
		ResultadoComparacao pessimista = rodarEstrategia("PESSIMISTA (PESSIMISTIC_WRITE)", this::confirmarPessimista);

		System.out.println("=== Comparação lock pessimista vs. UPDATE atômico condicional (N=" + N_THREADS
				+ "/M=1 vaga) - specs/012, Task 4 transcreve estes números para docs/DECISIONS.md ===");
		System.out.println(atomica);
		System.out.println(pessimista);

		assertCorretude(atomica);
		assertCorretude(pessimista);
	}

	private void assertCorretude(ResultadoComparacao resultado) {
		assertThat(resultado.sucessos()).as(resultado.nome() + ": exatamente 1 sucesso").isEqualTo(1);
		assertThat(resultado.sucessos() + resultado.conflitos())
				.as(resultado.nome() + ": todas as " + N_THREADS + " threads concluem sem exceção não tratada")
				.isEqualTo(N_THREADS);
	}

	/**
	 * Achado ao rodar o teste pela primeira vez (não estava no plano original de
	 * specs/012): turmaRepository.consumirVaga usa
	 * @Modifying(flushAutomatically = true), que exige uma transação ativa - chamado
	 * "puro" (sem esta wrapper), toda thread falhava com
	 * InvalidDataAccessApiUsageException("No EntityManager with actual transaction
	 * available..."), 10 de 10 vezes. Em produção isso nunca aparece porque
	 * MatriculaService.confirmar() já é @Transactional; aqui, chamando o repository
	 * diretamente (por desenho, para não passar pela camada de service/MockMvc na
	 * corrida em si), é preciso abrir essa mesma fronteira transacional manualmente.
	 * TransactionTemplate (não @Transactional no método de teste) para não acoplar a
	 * transação ao método inteiro nem à thread de teste - cada thread da corrida abre
	 * e fecha sua própria transação, exatamente como aconteceria com um
	 * @Transactional por chamada.
	 */
	private boolean confirmarAtomico(Long turmaId, long versionCapturada) {
		Boolean sucesso = new TransactionTemplate(transactionManager)
				.execute(status -> turmaRepository.consumirVaga(turmaId, versionCapturada) == 1);
		return Boolean.TRUE.equals(sucesso);
	}

	private boolean confirmarPessimista(Long turmaId, long versionCapturadaIgnorada) {
		return confirmacaoPessimistaService.confirmarComLockPessimista(turmaId);
	}

	private ResultadoComparacao rodarEstrategia(String nome, Estrategia estrategia) throws Exception {
		Long turmaId = criarTurma(1);
		long versionCapturada = turmaRepository.findById(turmaId).orElseThrow().getVersion();
		CyclicBarrier barreira = new CyclicBarrier(N_THREADS);
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger conflitos = new AtomicInteger();
		AtomicInteger excecoesInesperadas = new AtomicInteger();
		try {
			List<Callable<Void>> tarefas = new ArrayList<>();
			for (int i = 0; i < N_THREADS; i++) {
				tarefas.add(() -> {
					barreira.await();
					try {
						if (estrategia.tentar(turmaId, versionCapturada)) {
							sucessos.incrementAndGet();
						} else {
							conflitos.incrementAndGet();
						}
					} catch (RuntimeException ex) {
						excecoesInesperadas.incrementAndGet();
					}
					return null;
				});
			}
			long inicioNanos = System.nanoTime();
			List<Future<Void>> futures = executor.invokeAll(tarefas);
			for (Future<Void> future : futures) {
				future.get();
			}
			long tempoTotalMs = (System.nanoTime() - inicioNanos) / 1_000_000;
			return new ResultadoComparacao(nome, sucessos.get(), conflitos.get(), excecoesInesperadas.get(),
					tempoTotalMs);
		} finally {
			executor.shutdown();
		}
	}

	private Long criarTurma(int limiteVagas) throws Exception {
		Long cursoId = criarCurso();
		Long disciplinaId = criarDisciplina();
		vincular(cursoId, disciplinaId);
		String codigo = "CMP-T-" + sufixoUnico();
		String corpo = "{\"codigo\":\"" + codigo + "\",\"cursoId\":" + cursoId + ",\"disciplinaId\":" + disciplinaId
				+ ",\"limiteVagas\":" + limiteVagas + "}";
		String resposta = mockMvc
				.perform(post("/api/turmas").with(secretaria()).contentType("application/json").content(corpo))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarCurso() throws Exception {
		String codigo = "CMP-C-" + sufixoUnico();
		String resposta = mockMvc
				.perform(post("/api/cursos").with(secretaria()).contentType("application/json")
						.content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Curso Comparação\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private Long criarDisciplina() throws Exception {
		String codigo = "CMP-D-" + sufixoUnico();
		String resposta = mockMvc
				.perform(post("/api/disciplinas").with(secretaria()).contentType("application/json")
						.content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Disciplina Comparação\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return idDe(resposta);
	}

	private void vincular(Long cursoId, Long disciplinaId) throws Exception {
		mockMvc.perform(post("/api/cursos/" + cursoId + "/disciplinas/" + disciplinaId).with(secretaria()))
				.andExpect(status().isOk());
	}

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

	@FunctionalInterface
	private interface Estrategia {
		boolean tentar(Long turmaId, long versionCapturada);
	}

	private record ResultadoComparacao(String nome, int sucessos, int conflitos, int excecoesInesperadas,
			long tempoTotalMs) {
	}

}
