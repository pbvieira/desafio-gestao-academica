# Concorrência e testes consolidados (Fase 6 unificada) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provar com evidência real (não teórica) que a proteção de vaga contra matrícula concorrente
(D024/D025, já em produção) se comporta corretamente sob disputa real de 20 alunos pela última vaga de uma
turma, comparar numericamente essa estratégia com lock pessimista, e fechar as lacunas reais de
cobertura/observabilidade/documentação que restam. Sem funcionalidade de negócio nova.

**Architecture:** Prova e2e nova via Playwright `APIRequestContext` (`e2e/playwright/`, projeto Node
isolado, sem browser) contra a API real + Keycloak real. Comparação de lock pessimista isolada em
`src/test/java` (nunca em produção). Métrica Micrometer nova em `MatriculaService`. Documentação final na
spec 012 + README + ROADMAP.

**Tech Stack:** Playwright `@playwright/test` (Node 22, TypeScript), JUnit 5 + Testcontainers (já
estabelecido), Micrometer (já estabelecido), Grafana (dashboard JSON já existente).

**Numeração das tasks:** este plano começa na Task 2 porque a Task 1 (spec 012 + decisões D049-D052) já foi
concluída nesta sessão, fora deste arquivo — `specs/012-concorrencia-e-testes.md` está aprovada e
`docs/DECISIONS.md` já tem D049-D052 confirmados. Mantida a numeração 2-7 (em vez de renumerar para 1-6)
para bater com as referências já escritas na spec e no log de decisões.

## Global Constraints

- Nenhuma funcionalidade de negócio nova — só prova/comparação/observabilidade/documentação.
- Projeto Playwright em `e2e/playwright/`, isolado, só `@playwright/test` como devDependency, sem
  instalação de browser (D049).
- Repetição 10x da prova e2e via `--repeat-each=10` nativo do Playwright, `workers: 1` (D049).
- Teardown do e2e via `test.afterEach`: curso e disciplina são fixos e reaproveitados (nunca tocados no
  teardown); turma e os 20 alunos de cada repetição são sempre soft-deletados via `DELETE`, best-effort
  (log-and-continue por chamada) (D050).
- Curso fixo: código `E2E-CONC-C`. Disciplina fixa: código `E2E-CONC-D`. Criação idempotente: um 409 na
  criação é tratado como "já existe", buscando o id via `GET` + filtro por `codigo` (mesmo padrão de
  `e2e/matricula-flow.sh`).
- Código do lock pessimista (repository + serviço) só em `src/test/java`, pacote
  `br.com.desafio.tecnico.gestao.academico.concorrencia` — nunca em `src/main/java` (D051).
- Métrica nova: `matricula.vaga.conflito`, `Counter` com tag `motivo` (valores possíveis:
  `VAGAS_ESGOTADAS`, `CONFLITO_CONCORRENCIA`) (D052).
- Painel novo no dashboard Grafana já existente (`docker/grafana/provisioning/dashboards/jvm-http-dashboard.json`)
  — não criar dashboard novo (D009).
- `errorCode` é o nome exato do campo no corpo JSON de erro 409 (`ProblemDetailFactory`).
- Client Keycloak para obter token: `gestao-frontend` (`directAccessGrantsEnabled: true`, D036). Usuário:
  `secretaria.teste`/`secretaria123`.
- Portas locais default: app `:8080`, Keycloak `:8081` (`KEYCLOAK_HTTP_PORT`).
- Commits em português, estilo já usado no repo (`feat:`, `test:`, `docs:`, `chore:` + descrição curta).
- Ao verificar com Docker/app local, seguir o mesmo padrão já usado nos e2e existentes: `docker compose up`
  (perfil `observability` se for testar o painel Grafana) + `./mvnw spring-boot:run`.

---

### Task 2: E2E Playwright — disputa de 20 alunos pela última vaga (prioridade 1, entrega principal)

**Files:**
- Create: `e2e/playwright/package.json`
- Create: `e2e/playwright/playwright.config.ts`
- Create: `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts`
- Create: `e2e/playwright/.gitignore`
- Modify: `.github/workflows/ci.yml` (novo step no job `build`)

**Interfaces:**
- Consumes: API REST já existente (`POST /api/cursos`, `POST /api/disciplinas`,
  `POST /api/cursos/{id}/disciplinas/{disciplinaId}`, `POST /api/turmas`, `POST /api/alunos`,
  `POST /api/matriculas`, `POST /api/matriculas/{id}/confirmar`, `DELETE /api/turmas/{id}`,
  `DELETE /api/alunos/{id}`, `GET /api/cursos`, `GET /api/disciplinas`), endpoint de token do Keycloak.
- Produces: nada consumido por outras tasks deste plano (a Task 4 só precisa dos números impressos no
  output do teste, não de um artefato de código).

- [ ] **Step 1: Criar `e2e/playwright/package.json`**

```json
{
  "name": "e2e-concorrencia",
  "version": "1.0.0",
  "private": true,
  "description": "Prova e2e de concorrencia (disputa de 20 alunos pela ultima vaga) - specs/012-concorrencia-e-testes.md",
  "scripts": {
    "test": "playwright test"
  },
  "devDependencies": {
    "@playwright/test": "^1.48.0"
  }
}
```

- [ ] **Step 2: Instalar dependências e confirmar que não há download de browser (risco nomeado em D049)**

```bash
cd e2e/playwright && npm install
```

Expected: `node_modules/@playwright/test` instalado, `package-lock.json` gerado. Confirme que **não**
apareceu nenhuma mensagem de download de Chromium/Firefox/WebKit no output, e que `~/.cache/ms-playwright`
não foi criado ou não cresceu (`ls ~/.cache/ms-playwright 2>/dev/null` antes/depois do `npm install`, para
comparar). Se algum browser for baixado, pare e reporte como concern — o desenho do Step 6 (CI) assume que
isso não acontece.

- [ ] **Step 3: Criar `e2e/playwright/playwright.config.ts`**

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    fullyParallel: false,
    workers: 1,
    reporter: 'list',
    timeout: 60_000,
    use: {
        extraHTTPHeaders: {
            'Content-Type': 'application/json',
        },
    },
});
```

Nota: sem `use.baseURL` de propósito — o teste chama tanto a API da aplicação (`:8080`) quanto o Keycloak
(`:8081`), origens diferentes, então cada chamada usa URL absoluta (ver Step 4). `workers: 1` e
`fullyParallel: false` (D049): a concorrência real de 20 confirmações já acontece dentro do teste via
`Promise.all` — as 10 repetições (`--repeat-each=10`) rodam em série para facilitar diagnóstico caso alguma
falhe.

- [ ] **Step 4: Criar `e2e/playwright/tests/matricula-concorrencia-20-alunos.spec.ts`**

```typescript
import { test, expect, APIRequestContext } from '@playwright/test';

const KEYCLOAK_URL = process.env.KEYCLOAK_URL ?? 'http://localhost:8081';
const APP_URL = process.env.APP_URL ?? 'http://localhost:8080';

const CURSO_CODIGO = 'E2E-CONC-C';
const DISCIPLINA_CODIGO = 'E2E-CONC-D';
const N_ALUNOS = 20;

function headers(token: string) {
    return { Authorization: `Bearer ${token}` };
}

async function obterToken(request: APIRequestContext, username: string, password: string): Promise<string> {
    const resposta = await request.post(`${KEYCLOAK_URL}/realms/gestao/protocol/openid-connect/token`, {
        form: {
            grant_type: 'password',
            client_id: 'gestao-frontend',
            username,
            password,
            scope: 'openid',
        },
    });
    expect(resposta.ok(), `obter token de ${username}`).toBeTruthy();
    const corpo = await resposta.json();
    return corpo.access_token as string;
}

async function garantirCursoFixo(request: APIRequestContext, token: string): Promise<number> {
    const criar = await request.post(`${APP_URL}/api/cursos`, {
        headers: headers(token),
        data: { codigo: CURSO_CODIGO, nome: 'Curso E2E Concorrência' },
    });
    if (criar.status() === 409) {
        const lista = await request.get(`${APP_URL}/api/cursos`, { headers: headers(token) });
        const cursos = await lista.json();
        const existente = cursos.find((c: { codigo: string }) => c.codigo === CURSO_CODIGO);
        if (!existente) {
            throw new Error('curso fixo retornou 409 na criação mas não foi encontrado em GET /api/cursos');
        }
        return existente.id;
    }
    expect(criar.status(), 'criar curso fixo').toBe(201);
    return (await criar.json()).id;
}

async function garantirDisciplinaFixa(request: APIRequestContext, token: string): Promise<number> {
    const criar = await request.post(`${APP_URL}/api/disciplinas`, {
        headers: headers(token),
        data: { codigo: DISCIPLINA_CODIGO, nome: 'Disciplina E2E Concorrência' },
    });
    if (criar.status() === 409) {
        const lista = await request.get(`${APP_URL}/api/disciplinas`, { headers: headers(token) });
        const disciplinas = await lista.json();
        const existente = disciplinas.find((d: { codigo: string }) => d.codigo === DISCIPLINA_CODIGO);
        if (!existente) {
            throw new Error('disciplina fixa retornou 409 na criação mas não foi encontrada em GET /api/disciplinas');
        }
        return existente.id;
    }
    expect(criar.status(), 'criar disciplina fixa').toBe(201);
    return (await criar.json()).id;
}

async function garantirVinculo(request: APIRequestContext, token: string, cursoId: number, disciplinaId: number) {
    // CursoService.vincularDisciplina usa Set<Disciplina> - chamar de novo com o mesmo par é
    // idempotente (sem 409), então não precisa de tratamento especial de "já vinculado".
    const vincular = await request.post(`${APP_URL}/api/cursos/${cursoId}/disciplinas/${disciplinaId}`, {
        headers: headers(token),
    });
    expect(vincular.ok(), 'vincular disciplina ao curso').toBeTruthy();
}

let turmaParaTeardown: number | null = null;
let alunosParaTeardown: number[] = [];
let tokenParaTeardown: string | null = null;

test.afterEach(async ({ request }) => {
    if (!tokenParaTeardown) {
        return;
    }
    const token = tokenParaTeardown;
    if (turmaParaTeardown !== null) {
        const resposta = await request.delete(`${APP_URL}/api/turmas/${turmaParaTeardown}`, { headers: headers(token) });
        if (!resposta.ok()) {
            console.warn(`teardown: falha ao excluir turma ${turmaParaTeardown} (status ${resposta.status()})`);
        }
    }
    await Promise.all(
        alunosParaTeardown.map(async (alunoId) => {
            const resposta = await request.delete(`${APP_URL}/api/alunos/${alunoId}`, { headers: headers(token) });
            if (!resposta.ok()) {
                console.warn(`teardown: falha ao excluir aluno ${alunoId} (status ${resposta.status()})`);
            }
        }),
    );
    turmaParaTeardown = null;
    alunosParaTeardown = [];
    tokenParaTeardown = null;
});

test('disputa de 20 alunos pela última vaga: exatamente 1 confirmação vence, 19 recebem 409 VAGAS_ESGOTADAS', async ({
    request,
}, testInfo) => {
    const tokenSecretaria = await obterToken(request, 'secretaria.teste', 'secretaria123');
    tokenParaTeardown = tokenSecretaria;

    const cursoId = await garantirCursoFixo(request, tokenSecretaria);
    const disciplinaId = await garantirDisciplinaFixa(request, tokenSecretaria);
    await garantirVinculo(request, tokenSecretaria, cursoId, disciplinaId);

    // "E2E-CT-" (7 chars) + sufixo precisa caber no limite de 20 chars do campo código
    // (mesmo achado documentado em MatriculaConcorrenciaIntegrationTest.java e
    // e2e/matricula-flow.sh) - 8 dígitos de Date.now()%1e8 + 1 dígito de repeatEachIndex.
    const sufixo = `${Date.now() % 100_000_000}${testInfo.repeatEachIndex}`;
    const turmaCodigo = `E2E-CT-${sufixo}`;

    const criarTurma = await request.post(`${APP_URL}/api/turmas`, {
        headers: headers(tokenSecretaria),
        data: { codigo: turmaCodigo, cursoId, disciplinaId, limiteVagas: 1 },
    });
    expect(criarTurma.status(), 'criar turma com 1 vaga').toBe(201);
    const turmaId = (await criarTurma.json()).id;
    turmaParaTeardown = turmaId;

    const alunoIds: number[] = [];
    for (let i = 0; i < N_ALUNOS; i++) {
        const criarAluno = await request.post(`${APP_URL}/api/alunos`, {
            headers: headers(tokenSecretaria),
            data: {
                nome: `Aluno E2E Concorrência ${i}`,
                email: `aluno.conc.${sufixo}.${i}@example.com`,
            },
        });
        expect(criarAluno.status(), `criar aluno ${i}`).toBe(201);
        const alunoId = (await criarAluno.json()).id;
        alunoIds.push(alunoId);
        alunosParaTeardown.push(alunoId);
    }

    const matriculaIds: number[] = [];
    for (const alunoId of alunoIds) {
        const criarMatricula = await request.post(`${APP_URL}/api/matriculas`, {
            headers: headers(tokenSecretaria),
            data: { alunoId, turmaId },
        });
        expect(criarMatricula.status(), `criar matrícula PENDENTE para aluno ${alunoId}`).toBe(201);
        matriculaIds.push((await criarMatricula.json()).id);
    }

    // O ponto central da prova: as 20 confirmações são disparadas de uma vez via
    // Promise.all, sem await sequencial entre elas - garante concorrência real, não
    // uma simulação sequencial disfarçada.
    const confirmacoes = await Promise.all(
        matriculaIds.map((matriculaId) =>
            request.post(`${APP_URL}/api/matriculas/${matriculaId}/confirmar`, { headers: headers(tokenSecretaria) }),
        ),
    );

    const sucessos = confirmacoes.filter((r) => r.status() === 200);
    const conflitos = confirmacoes.filter((r) => r.status() === 409);
    const outros = confirmacoes.filter((r) => r.status() !== 200 && r.status() !== 409);

    expect(outros.length, 'nenhuma confirmação deve retornar status inesperado (nem exceção/timeout)').toBe(0);
    expect(sucessos.length, 'exatamente 1 confirmação deve ter sucesso').toBe(1);
    expect(conflitos.length, 'exatamente 19 confirmações devem receber 409').toBe(19);

    for (const conflito of conflitos) {
        const corpo = await conflito.json();
        expect(corpo.errorCode, 'motivo do conflito deve ser vaga esgotada, não outro tipo de 409').toBe(
            'VAGAS_ESGOTADAS',
        );
    }
});
```

- [ ] **Step 5: Criar `e2e/playwright/.gitignore`**

```
node_modules
test-results
playwright-report
```

- [ ] **Step 6: Rodar localmente contra a stack de verdade**

Pré-requisito: `docker compose up -d` (Keycloak + Postgres) e `./mvnw spring-boot:run` (app) já de pé,
igual ao pressuposto dos scripts em `e2e/`.

```bash
cd e2e/playwright && npx playwright test --repeat-each=10
```

Expected: 10/10 repetições passam (`10 passed`), cada uma reportada individualmente
(`... (repeat:N/10)`), sem timeout. Se alguma falhar, o relatório aponta exatamente qual repetição e por
quê — investigue antes de prosseguir, não ignore uma falha intermitente.

- [ ] **Step 7: Verificar teardown funcionou (turma e alunos da última repetição não aparecem mais como ativos)**

```bash
# Substitua <ultima-turma-codigo-do-log> pelo código impresso na última repetição, ou confira via:
curl -s http://localhost:8080/api/turmas -H "Authorization: Bearer $(...)" | grep -c "E2E-CT-"
```

Expected: nenhuma turma com prefixo `E2E-CT-` aparece na listagem (todas soft-deletadas pelo `afterEach`).
Curso/disciplina fixos (`E2E-CONC-C`/`E2E-CONC-D`) continuam existindo — isso é esperado (D050), não é bug.

- [ ] **Step 8: Adicionar o step ao CI, no job `build` de `.github/workflows/ci.yml`, logo após o e2e de administração**

Localize:

```yaml
      - name: E2E administracao de usuarios/papeis (specs/010)
        run: bash e2e/administracao-papel-flow.sh
```

Adicione logo depois (antes de `Diagnostico em caso de falha`):

```yaml
      - name: Configurar Node 22 para o e2e de concorrencia (specs/012, D049)
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: e2e/playwright/package-lock.json

      - name: Instalar dependencias do e2e de concorrencia
        working-directory: e2e/playwright
        run: npm ci

      - name: E2E concorrencia - disputa de 20 alunos pela ultima vaga, 10x (specs/012, D024/D049/D050)
        working-directory: e2e/playwright
        run: npx playwright test --repeat-each=10
```

Nenhum step de `playwright install` — o Step 2 já confirmou que `@playwright/test` com `APIRequestContext`
não baixa browser.

- [ ] **Step 9: Commit**

```bash
git add e2e/playwright .github/workflows/ci.yml
git commit -m "test: prova e2e da disputa de 20 alunos pela ultima vaga (Playwright, specs/012)"
```

---

### Task 3: Comparação com lock pessimista — N=10/M=1 (prioridade 2, paralelizável com a Task 2)

**Files:**
- Create: `src/test/java/br/com/desafio/tecnico/gestao/academico/concorrencia/TurmaLockPessimistaRepository.java`
- Create: `src/test/java/br/com/desafio/tecnico/gestao/academico/concorrencia/ConfirmacaoPessimistaService.java`
- Create: `src/test/java/br/com/desafio/tecnico/gestao/academico/concorrencia/LockPessimistaVsAtomicoComparativoIntegrationTest.java`

**Interfaces:**
- Consumes: `TurmaRepository.consumirVaga(Long, long)` (já existe, produção), `Turma` (domínio, já existe).
- Produces: nenhum símbolo consumido por outra task — a Task 4 só precisa do output impresso no console do
  teste (`System.out.println`), transcrito manualmente para `docs/DECISIONS.md`.

- [ ] **Step 1: Criar `TurmaLockPessimistaRepository.java`**

```java
package br.com.desafio.tecnico.gestao.academico.concorrencia;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.desafio.tecnico.gestao.academico.domain.Turma;

/**
 * specs/012-concorrencia-e-testes.md, D051: existe só para comparar números com a
 * estratégia atômica já em produção (D024) - nunca referenciado por código de
 * src/main/java. Repository de teste dedicado, descoberto automaticamente pelo
 * component-scan do Spring Boot (br.com.desafio.tecnico.gestao.* sem restrição), sem
 * nenhuma configuração adicional.
 */
public interface TurmaLockPessimistaRepository extends JpaRepository<Turma, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Turma t where t.id = :id")
	Optional<Turma> buscarComLockPessimista(@Param("id") Long id);

}
```

- [ ] **Step 2: Criar `ConfirmacaoPessimistaService.java`**

```java
package br.com.desafio.tecnico.gestao.academico.concorrencia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.domain.Turma;

/**
 * specs/012-concorrencia-e-testes.md, D051: réplica mínima só do ponto de acesso à vaga
 * que TurmaRepository.consumirVaga (D024, UPDATE condicional atômico) substitui em
 * produção - lê a Turma sob lock pessimista, checa o limite em Java, incrementa. Não
 * duplica MatriculaService inteiro (sem status de Matrícula, sem eventos) - existe só
 * para medir o ponto de contenção isoladamente.
 */
@Service
public class ConfirmacaoPessimistaService {

	private final TurmaLockPessimistaRepository turmaLockPessimistaRepository;

	public ConfirmacaoPessimistaService(TurmaLockPessimistaRepository turmaLockPessimistaRepository) {
		this.turmaLockPessimistaRepository = turmaLockPessimistaRepository;
	}

	/**
	 * @return true se a vaga foi consumida, false se o limite já havia sido atingido.
	 */
	@Transactional
	public boolean confirmarComLockPessimista(Long turmaId) {
		Turma turma = turmaLockPessimistaRepository.buscarComLockPessimista(turmaId)
				.orElseThrow(() -> new IllegalStateException("Turma '" + turmaId + "' não encontrada."));
		if (turma.getVagasOcupadas() >= turma.getLimiteVagas()) {
			return false;
		}
		turma.setVagasOcupadas(turma.getVagasOcupadas() + 1);
		return true;
	}

}
```

- [ ] **Step 3: Criar `LockPessimistaVsAtomicoComparativoIntegrationTest.java`**

```java
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
import org.testcontainers.containers.PostgreSQLContainer;
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

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.docker.compose.enabled", () -> "false");
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TurmaRepository turmaRepository;

	@Autowired
	private ConfirmacaoPessimistaService confirmacaoPessimistaService;

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

	private boolean confirmarAtomico(Long turmaId, long versionCapturada) {
		return turmaRepository.consumirVaga(turmaId, versionCapturada) == 1;
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
```

- [ ] **Step 4: Rodar o teste e confirmar que os beans de teste foram descobertos (risco nomeado em D051)**

```bash
./mvnw test -Dtest=LockPessimistaVsAtomicoComparativoIntegrationTest
```

Expected: teste passa; se aparecer `NoSuchBeanDefinitionException` para `TurmaLockPessimistaRepository` ou
`ConfirmacaoPessimistaService`, o component-scan implícito não alcançou o pacote — nesse caso (não
esperado, mas trate como possível), adicione uma classe `@TestConfiguration` com `@Import` explícito só
neste teste, sem tocar em `src/main/java`. Capture o output do console (`System.out.println` da linha
`=== Comparação ... ===` em diante) — a Task 4 precisa desses números exatos.

- [ ] **Step 5: Confirmar que nenhum código de lock pessimista vazou para produção**

```bash
grep -r "PESSIMISTIC_WRITE" src/main/java/ && echo "FALHA: achado em src/main" || echo "OK: nada em src/main"
```

Expected: `OK: nada em src/main`.

- [ ] **Step 6: Commit**

```bash
git add src/test/java/br/com/desafio/tecnico/gestao/academico/concorrencia
git commit -m "test: compara lock pessimista com UPDATE atomico condicional (N=10/M=1, specs/012, D051)"
```

---

### Task 4: Decisão final registrada (depende da Task 2 + Task 3 concluídas com números reais)

**Files:**
- Modify: `docs/DECISIONS.md` (nova entrada D053, próximo número livre após D052)
- Modify: `specs/012-concorrencia-e-testes.md` (seção 5, linha 5; seção 9)

**Interfaces:**
- Consumes: números reais impressos pela Task 3 (console) e resultado da Task 2 (10/10 repetições, e o
  tempo aproximado observado no Step 6 da Task 2).
- Produces: link `docs/DECISIONS.md#d053` referenciado pela spec 012.

- [ ] **Step 1: Coletar os números reais**

Releia o output do `System.out.println` do teste da Task 3 (Step 4) — copie os valores exatos de
`sucessos`, `conflitos`, `excecoesInesperadas`, `tempoTotalMs` para as duas estratégias. Releia o resultado
da Task 2 (10/10 repetições, sem falha).

- [ ] **Step 2: Perguntar ao Pablo antes de registrar — não decidir sozinho mesmo com o resultado esperado**

Apresente os números coletados e pergunte explicitamente: "manter a estratégia atômica (D024) ou trocar
para lock pessimista?" — mesmo que os números favoreçam a atômica (resultado esperado, dado que já é a
estratégia validada em produção desde a spec 006), a confirmação é do Pablo, não da IA.

- [ ] **Step 3: Registrar `D053` em `docs/DECISIONS.md`**

Adicione ao índice (após a linha do D052) e como nova entrada ao final do arquivo, seguindo o template já
usado nas entradas anteriores (`## D053 — <título>`, com **Data**, **Origem**, **Spec relacionada**,
**Contexto**, **Alternativas consideradas**, **Decisão**, **Justificativa**, **Trade-offs aceitos**,
**Riscos conhecidos**). Preencha **Contexto** com o cenário da comparação (N=10/M=1, mais a prova e2e de
20 alunos/10 repetições), **Alternativas consideradas** com as duas estratégias e os números reais
coletados no Step 1, **Decisão** com a resposta do Pablo ao Step 2. Se a atômica vencer (esperado):
**Trade-offs aceitos**/**Riscos conhecidos** devem mencionar explicitamente que nenhum código de produção
precisa ser removido (D051 já garantiu isso — o harness pessimista fica em `src/test/java` como benchmark
permanente, não porque a remoção foi pulada, mas porque nunca houve nada em produção para remover). Se a
pessimista vencer (inesperado): registre isso como achado, e anote explicitamente que migrar
`MatriculaService.confirmar()` para essa estratégia é um follow-up separado, fora do escopo desta fase —
não implemente a migração.

- [ ] **Step 4: Atualizar `specs/012-concorrencia-e-testes.md`**

Seção 5, linha 5 (coluna "Decisão"): troque "pendente — registrada em entrada própria..." pelo link
`[D053](../docs/DECISIONS.md#d053) — <resumo de uma linha da decisão>`. Seção 9 (Definition of Done): no
item sobre `docs/DECISIONS.md`, remova a ressalva "decisão final da Task 4 pendente".

- [ ] **Step 5: Commit**

```bash
git add docs/DECISIONS.md specs/012-concorrencia-e-testes.md
git commit -m "docs: registra decisao final da comparacao atomica vs pessimista (D053, specs/012)"
```

---

### Task 5: Auditoria de cobertura reduzida (independente, pode rodar em paralelo com 2/3)

**Files:**
- Modify: TBD pelo relatório JaCoCo — provavelmente arquivos novos/estendidos em
  `src/test/java/br/com/desafio/tecnico/gestao/administracao/` e/ou `academico/`.

**Interfaces:**
- Consumes: nada de código de outras tasks deste plano.
- Produces: nada consumido por outras tasks.

- [ ] **Step 1: Gerar o relatório JaCoCo**

```bash
./mvnw clean verify
```

Expected: build verde, gate de cobertura ≥80% já mantido (nenhuma mudança de código de produção nesta
task deveria derrubar o gate). Abra `target/site/jacoco/index.html` num navegador (ou
`target/site/jacoco/br.com.desafio.tecnico.gestao.administracao/index.html` diretamente para o módulo mais
recente).

- [ ] **Step 2: Listar lacunas reais (não gerar teste antes de ter essa lista)**

No relatório da Task Report (o arquivo de report desta task, ver instruções do controlador), liste
explicitamente: (a) qualquer regra do PRD §02 sem cobertura de teste — improvável, mas confira; (b)
caminhos de erro (400/401/403/404/409/500) não testados no módulo `administracao`, em particular: o risco
conhecido documentado em D048 sobre `AdministracaoUsuarioService.reatribuirPapel()` fazer remove-then-add
não atômico (verifique em
`src/test/java/br/com/desafio/tecnico/gestao/administracao/service/AdministracaoUsuarioServiceTest.java`
se existe um teste que force `add()` falhar depois de `remove()` suceder — se não existir, esse é um
candidato real a teste novo). Não liste linhas triviais (getter/setter Lombok, construtores) como lacuna.

- [ ] **Step 3: Escrever só os testes que cobrem lacunas nomeadas no Step 2**

Cada teste novo deve ter, no commit ou no relatório da task, uma frase explícita conectando-o a um item da
lista do Step 2 (ex: "cobre o caminho de erro X, listado no Step 2"). Se, depois do Step 2, a lista estiver
vazia (não há lacuna real digna de nota), reporte isso explicitamente e não escreva teste nenhum só para
"ter algo a mostrar" — isso é o resultado esperado e aceitável desta task.

- [ ] **Step 4: Rodar `./mvnw clean verify` de novo, confirmar gate mantido**

```bash
./mvnw clean verify
```

Expected: build verde, cobertura ≥80% mantida (ou melhorada pelos testes novos, se houver).

- [ ] **Step 5: Commit (só se houve teste novo)**

```bash
git add src/test/java
git commit -m "test: cobre lacunas reais identificadas na auditoria JaCoCo (specs/012)"
```

---

### Task 6: Métrica Micrometer + painel Grafana (depende só da decisão de nome/tag já confirmada, D052)

**Files:**
- Modify: `src/main/java/br/com/desafio/tecnico/gestao/academico/service/MatriculaService.java`
- Modify: `src/test/java/br/com/desafio/tecnico/gestao/academico/service/MatriculaServiceTest.java`
- Modify: `docker/grafana/provisioning/dashboards/jvm-http-dashboard.json`

**Interfaces:**
- Consumes: `io.micrometer.core.instrument.MeterRegistry` (já uma dependência do projeto, usada em
  `MatriculaNotificacaoListener`).
- Produces: contador Prometheus `matricula_vaga_conflito_total{motivo="..."}` (nome com underscore é a
  conversão automática do Micrometer para o formato Prometheus) — consumido só pelo painel Grafana desta
  mesma task.

- [ ] **Step 1: Escrever o teste primeiro (TDD) — estender os dois testes de conflito já existentes em `MatriculaServiceTest.java`**

Troque o campo `@InjectMocks private MatriculaService matriculaService;` e a anotação `import
org.mockito.InjectMocks;` (remova o import, não é mais usado) por wiring manual, seguindo o mesmo padrão já
usado em `MatriculaNotificacaoListenerTest.java`:

```java
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
// (remova "import org.mockito.InjectMocks;")
```

```java
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private MatriculaService matriculaService;

	@BeforeEach
	void setUp() {
		matriculaService = new MatriculaService(matriculaRepository, turmaRepository, alunoRepository,
				eventPublisher, tracer, meterRegistry);
	}
```

(remova a linha `@InjectMocks private MatriculaService matriculaService;` que existia antes). Depois,
estenda os dois testes de conflito existentes com a asserção do contador:

```java
	@Test
	void confirmar_semVagaDisponivel_lancaConflitoVagasEsgotadas() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 30);
		Matricula matricula = matricula(1L, turma, StatusMatricula.PENDENTE);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.consumirVaga(10L, 0L)).thenReturn(0);
		when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 30)));

		assertThatThrownBy(() -> matriculaService.confirmar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("VAGAS_ESGOTADAS");
		verify(matriculaRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
		assertThat(meterRegistry.counter("matricula.vaga.conflito", "motivo", "VAGAS_ESGOTADAS").count())
				.isEqualTo(1.0);
	}
```

```java
	@Test
	void confirmar_conflitoDeVersaoSemVagasEsgotadas_lancaConflitoConcorrencia() {
		Turma turma = turma(10L, StatusTurma.ABERTA, 30, 20);
		Matricula matricula = matricula(1L, turma, StatusMatricula.PENDENTE);
		when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
		when(turmaRepository.consumirVaga(10L, 0L)).thenReturn(0);
		when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma(10L, StatusTurma.ABERTA, 30, 21)));

		assertThatThrownBy(() -> matriculaService.confirmar(1L)).isInstanceOf(ConflitoRegraNegocioException.class)
				.extracting(ex -> ((ConflitoRegraNegocioException) ex).getErrorCode())
				.isEqualTo("CONFLITO_CONCORRENCIA");
		assertThat(meterRegistry.counter("matricula.vaga.conflito", "motivo", "CONFLITO_CONCORRENCIA").count())
				.isEqualTo(1.0);
	}
```

- [ ] **Step 2: Rodar os testes para confirmar que falham (RED) — construtor ainda não aceita `MeterRegistry`**

```bash
./mvnw test -Dtest=MatriculaServiceTest
```

Expected: FAIL — erro de compilação (`MatriculaService` não tem construtor com 6 parâmetros) ou, se
compilar por acidente, falha das duas asserções novas de contador.

- [ ] **Step 3: Implementar — injetar `MeterRegistry` em `MatriculaService` e incrementar o contador**

Em `MatriculaService.java`, adicione o import e o campo/parâmetro de construtor:

```java
import io.micrometer.core.instrument.MeterRegistry;
```

```java
	private final MatriculaRepository matriculaRepository;
	private final TurmaRepository turmaRepository;
	private final AlunoRepository alunoRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Tracer tracer;
	private final MeterRegistry meterRegistry;

	public MatriculaService(MatriculaRepository matriculaRepository, TurmaRepository turmaRepository,
			AlunoRepository alunoRepository, ApplicationEventPublisher eventPublisher, Tracer tracer,
			MeterRegistry meterRegistry) {
		this.matriculaRepository = matriculaRepository;
		this.turmaRepository = turmaRepository;
		this.alunoRepository = alunoRepository;
		this.eventPublisher = eventPublisher;
		this.tracer = tracer;
		this.meterRegistry = meterRegistry;
	}
```

No método `confirmar()`, dentro do bloco `if (linhasAfetadas == 0) { ... }`, incremente o contador logo
antes de cada `throw` (tag `motivo` = o mesmo `errorCode` já usado na exceção — D052):

```java
		if (linhasAfetadas == 0) {
			Turma turmaAtual = turmaRepository.findById(turma.getId())
					.orElseThrow(() -> new RecursoNaoEncontradoException("Turma '" + turma.getId() + "' não encontrada."));
			if (turmaAtual.getVagasOcupadas() >= turmaAtual.getLimiteVagas()) {
				meterRegistry.counter("matricula.vaga.conflito", "motivo", "VAGAS_ESGOTADAS").increment();
				throw new ConflitoRegraNegocioException(
						"Não há vagas disponíveis na turma '" + turmaAtual.getCodigo() + "'.", "VAGAS_ESGOTADAS");
			}
			meterRegistry.counter("matricula.vaga.conflito", "motivo", "CONFLITO_CONCORRENCIA").increment();
			throw new ConflitoRegraNegocioException(
					"Conflito de concorrência ao confirmar a matrícula '" + id + "'. Tente novamente.",
					"CONFLITO_CONCORRENCIA");
		}
```

- [ ] **Step 4: Rodar os testes de novo, confirmar GREEN**

```bash
./mvnw test -Dtest=MatriculaServiceTest
```

Expected: todos os testes da classe passam, incluindo os dois novos.

- [ ] **Step 5: Adicionar o painel ao dashboard Grafana existente**

Em `docker/grafana/provisioning/dashboards/jvm-http-dashboard.json`, adicione um 5º objeto ao array
`panels` (depois do painel `id: 4`):

```json
    {
      "id": 5,
      "title": "Conflitos de vaga na confirmação de matrícula / s",
      "type": "timeseries",
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 16 },
      "datasource": { "type": "prometheus", "uid": "prometheus-uid" },
      "targets": [
        {
          "datasource": { "type": "prometheus", "uid": "prometheus-uid" },
          "expr": "sum(rate(matricula_vaga_conflito_total{application=\"gestao\"}[1m])) by (motivo)",
          "legendFormat": "{{motivo}}"
        }
      ]
    }
```

- [ ] **Step 6: Verificar manualmente com dado real**

```bash
docker compose --profile observability up -d
./mvnw spring-boot:run &
# em outro terminal, dispare um conflito real (ex: rode a Task 2 do e2e, ou reuse
# e2e/matricula-flow.sh duas vezes seguidas na mesma turma sem vaga) e confirme:
curl -s http://localhost:8080/actuator/prometheus | grep matricula_vaga_conflito
```

Expected: a métrica aparece com pelo menos uma amostra e a tag `motivo`. Abra o Grafana
(`http://localhost:${GRAFANA_PORT:-3000}`, dashboard "Gestão - JVM & HTTP (básico)") e confirme
visualmente que o novo painel renderiza com dado real.

- [ ] **Step 7: Rodar a suíte completa e commit**

```bash
./mvnw clean verify
```

```bash
git add src/main/java/br/com/desafio/tecnico/gestao/academico/service/MatriculaService.java \
        src/test/java/br/com/desafio/tecnico/gestao/academico/service/MatriculaServiceTest.java \
        docker/grafana/provisioning/dashboards/jvm-http-dashboard.json
git commit -m "feat: metrica matricula.vaga.conflito com tag motivo + painel Grafana (specs/012, D052)"
```

---

### Task 7: Documentação final (depende de todas as tasks anteriores)

**Files:**
- Modify: `specs/012-concorrencia-e-testes.md` (seção 10, checklist da seção 9)
- Modify: `docs/ROADMAP.md` (Fase 6 → Concluída)
- Modify: `README.md` (seção de concorrência + como rodar `e2e/playwright/`)

**Interfaces:**
- Consumes: números reais e decisão da Task 4, resultado da Task 2.
- Produces: nada — última task do plano.

- [ ] **Step 1: Preencher `specs/012-concorrencia-e-testes.md`, seção 10**

Substitua os placeholders `<A preencher...>` pelos números reais: quantas das 10 repetições da Task 2
passaram (deve ser 10/10), o resultado de cada uma (1×200/19×409), os números da Task 3 (tempo total e
contagem de exceções por estratégia) e a decisão final da Task 4 com o link `D053`. Preencha a resposta
sobre "muitas instituições simultâneas" conectando com D002 (RabbitMQ) — a contenção de vaga é por linha de
Turma, não por instituição, então instituições diferentes não competem pela mesma linha; o gargalo real
seria o volume de escrita na mesma Turma popular, não o número de instituições.

- [ ] **Step 2: Marcar a checklist de Definition of Done (seção 9) e todos os critérios de aceite (seção 6)**

Troque cada `- [ ]` por `- [x]` conforme verificado nas tasks anteriores. Mude `**Status:**` de `aprovada`
para `concluída`.

- [ ] **Step 3: Atualizar `docs/ROADMAP.md`**

Na tabela da seção "Fases restantes", troque a linha da Fase 6 de `Pendente` para
`**Concluída** — spec 012 (6 tasks executadas via subagent-driven-development, revisão final de branch
aprovada em <data>; decisões D049-D053)`.

- [ ] **Step 4: Adicionar seção de concorrência ao `README.md`**

Adicione uma seção nova (ou subseção dentro de uma seção de arquitetura já existente, se fizer mais sentido
ao ler o README atual primeiro) descrevendo: o mecanismo de proteção de vaga (D024, `UPDATE` condicional
atômico), referência à prova e2e de 20 alunos (`e2e/playwright/`) e o resultado real, e um resumo de uma
frase da comparação com lock pessimista (D053).

- [ ] **Step 5: Documentar como rodar `e2e/playwright/` localmente**

Na seção do README que já documenta como rodar os testes/e2e existentes (`e2e/*.sh`), adicione:

```markdown
**E2E de concorrência (Playwright):** prova a disputa de 20 alunos pela última vaga de uma turma, 10
execuções consecutivas. Requer Node 22.

\`\`\`bash
cd e2e/playwright
npm install
npx playwright test --repeat-each=10
\`\`\`

Pressupõe a mesma stack já de pé que os demais scripts de `e2e/` (Postgres/Keycloak via `docker compose
up`, aplicação via `./mvnw spring-boot:run`).
```

- [ ] **Step 6: Commit**

```bash
git add specs/012-concorrencia-e-testes.md docs/ROADMAP.md README.md
git commit -m "docs: fecha spec 012 - numeros reais, ROADMAP e README (Fase 6 unificada)"
```

---

## Depois da Task 7

Revisão final de branch (whole-branch review): gere o pacote de diff desde o commit anterior à Task 1
(baseline já registrado no ledger `.superpowers/sdd/progress.md` desta sessão) até `HEAD`, e dispache
`code-reviewer` + `security-auditor` sobre o diff completo da fase, seguindo o mesmo processo já usado nas
specs 010/011 — isso é o último gate do `subagent-driven-development`, não uma task deste arquivo.
