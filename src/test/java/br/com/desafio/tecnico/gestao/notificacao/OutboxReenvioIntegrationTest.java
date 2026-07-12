package br.com.desafio.tecnico.gestao.notificacao;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import br.com.desafio.tecnico.gestao.GestaoApplication;
import br.com.desafio.tecnico.gestao.academico.domain.Aluno;
import br.com.desafio.tecnico.gestao.academico.domain.Curso;
import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.domain.Turma;
import br.com.desafio.tecnico.gestao.academico.dto.AlunoRequest;
import br.com.desafio.tecnico.gestao.academico.dto.CursoRequest;
import br.com.desafio.tecnico.gestao.academico.dto.DisciplinaRequest;
import br.com.desafio.tecnico.gestao.academico.dto.MatriculaRequest;
import br.com.desafio.tecnico.gestao.academico.dto.TurmaRequest;
import br.com.desafio.tecnico.gestao.academico.service.AlunoService;
import br.com.desafio.tecnico.gestao.academico.service.CursoService;
import br.com.desafio.tecnico.gestao.academico.service.DisciplinaService;
import br.com.desafio.tecnico.gestao.academico.service.MatriculaService;
import br.com.desafio.tecnico.gestao.academico.service.TurmaService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * specs/007-mensageria-rabbitmq.md, seção 4.3/4.6: prova as duas metades da garantia de
 * outbox - (1) uma falha de publicação externa (RabbitMQ fora do ar) deixa o registro em
 * `event_publication` incompleto, sem perder o evento nem falhar a transação de negócio;
 * (2) reiniciar a aplicação (`republish-outstanding-events-on-restart=true`) reenvia o
 * evento pendente. Usa dois contextos Spring reais (start → stop → start), não um mock
 * de reinício - é a única forma de testar o comportamento documentado da property sem
 * depender de API interna não documentada do Modulith.
 */
@Testcontainers
class OutboxReenvioIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	// Não usa @Container: precisa de controle manual de stop()/start() no meio do
	// teste, o que o ciclo de vida automático do JUnit não permite.
	static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-management"));

	@BeforeAll
	static void subirRabbitMq() {
		rabbitmq.start();
	}

	@AfterAll
	static void encerrarRabbitMq() {
		rabbitmq.stop();
	}

	@Test
	void eventoIncompleto_quandoRabbitIndisponivel_eReenviadoNoRestart() {
		Map<String, Object> propriedadesBase = new HashMap<>();
		propriedadesBase.put("spring.docker.compose.enabled", "false");
		// web-application-type=none quebraria SecurityConfig#actuatorPrometheusSecurityFilterChain
		// (exige HttpSecurity, só disponível com a camada web presente) - mantém o
		// default (servlet), só com porta aleatória para os dois starts sequenciais
		// não brigarem por 8080 (o primeiro contexto é fechado antes do segundo subir,
		// mas porta aleatória evita qualquer corrida residual do SO liberando a porta).
		propriedadesBase.put("server.port", "0");
		propriedadesBase.put("spring.datasource.url", postgres.getJdbcUrl());
		propriedadesBase.put("spring.datasource.username", postgres.getUsername());
		propriedadesBase.put("spring.datasource.password", postgres.getPassword());
		propriedadesBase.put("spring.modulith.events.republish-outstanding-events-on-restart", "true");

		// Testcontainers só expõe host/porta mapeada com o container rodando - captura
		// ANTES de derrubar (Docker preserva o mesmo mapeamento de porta num
		// stop()+start() do MESMO container, ao contrário de remover e recriar).
		String rabbitHost = rabbitmq.getHost();
		Integer rabbitPort = rabbitmq.getAmqpPort();
		String rabbitUser = rabbitmq.getAdminUsername();
		String rabbitPassword = rabbitmq.getAdminPassword();

		// 1ª execução: RabbitMQ derrubado ANTES de criar a matrícula - a publicação do
		// evento MatriculaCriada para o exchange falha, mas a criação da Matrícula em si
		// (transação de negócio) não é afetada - essa é a garantia central do outbox.
		rabbitmq.stop();
		Long matriculaId;
		try (ConfigurableApplicationContext contexto1 = iniciarContexto(propriedadesBase, rabbitHost, rabbitPort,
				rabbitUser, rabbitPassword)) {
			matriculaId = criarMatriculaDeTeste(contexto1);
		}

		JdbcTemplate jdbc = new JdbcTemplate(dataSourceDireto());
		Integer incompletas = jdbc.queryForObject(
				"SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL", Integer.class);
		assertThat(incompletas).as("publicação deve ficar incompleta com o RabbitMQ fora do ar").isGreaterThan(0);

		// 2ª execução: RabbitMQ de volta, novo contexto Spring - o startup real (não
		// simulado) deve reenviar a publicação pendente, dado
		// republish-outstanding-events-on-restart=true.
		rabbitmq.start();
		try (ConfigurableApplicationContext contexto2 = iniciarContexto(propriedadesBase, rabbitmq.getHost(),
				rabbitmq.getAmqpPort(), rabbitUser, rabbitPassword)) {
			Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
				Integer aindaIncompletas = jdbc.queryForObject(
						"SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL", Integer.class);
				assertThat(aindaIncompletas).as("publicação deve ser completada após o reenvio no restart")
						.isZero();
			});
		}

		assertThat(matriculaId).isNotNull();
	}

	private ConfigurableApplicationContext iniciarContexto(Map<String, Object> propriedadesBase, String rabbitHost,
			int rabbitPort, String rabbitUser, String rabbitPassword) {
		Map<String, Object> propriedades = new HashMap<>(propriedadesBase);
		propriedades.put("spring.rabbitmq.host", rabbitHost);
		propriedades.put("spring.rabbitmq.port", rabbitPort);
		propriedades.put("spring.rabbitmq.username", rabbitUser);
		propriedades.put("spring.rabbitmq.password", rabbitPassword);
		return new SpringApplicationBuilder(GestaoApplication.class).properties(propriedades).run();
	}

	private Long criarMatriculaDeTeste(ConfigurableApplicationContext contexto) {
		String sufixo = String.valueOf(System.nanoTime() % 1_000_000_000L);
		CursoService cursoService = contexto.getBean(CursoService.class);
		DisciplinaService disciplinaService = contexto.getBean(DisciplinaService.class);
		TurmaService turmaService = contexto.getBean(TurmaService.class);
		AlunoService alunoService = contexto.getBean(AlunoService.class);
		MatriculaService matriculaService = contexto.getBean(MatriculaService.class);

		Curso curso = cursoService.criar(new CursoRequest("OUT-C-" + sufixo, "Curso Outbox"));
		Disciplina disciplina = disciplinaService.criar(new DisciplinaRequest("OUT-D-" + sufixo, "Disciplina Outbox"));
		cursoService.vincularDisciplina(curso.getId(), disciplina.getId());
		Turma turma = turmaService
				.criar(new TurmaRequest("OUT-T-" + sufixo, curso.getId(), disciplina.getId(), 5));
		Aluno aluno = alunoService
				.criar(new AlunoRequest("Aluno Outbox", "aluno.outbox." + sufixo + "@example.com", null));

		return matriculaService.criar(new MatriculaRequest(aluno.getId(), turma.getId())).getId();
	}

	private DataSource dataSourceDireto() {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUrl(postgres.getJdbcUrl());
		dataSource.setUser(postgres.getUsername());
		dataSource.setPassword(postgres.getPassword());
		return dataSource;
	}

}
