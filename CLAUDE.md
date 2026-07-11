# CLAUDE.md

Este arquivo fornece orientação ao Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

## O que é este repositório

Este repositório é um desafio técnico (`PRD.md`, em português) para uma vaga de Desenvolvedor(a) Sênior Full Stack na Tribe Lyceum – Techne. A tarefa é construir um **sistema de gestão de matrículas acadêmicas**. Leia o `PRD.md` por completo antes de tomar decisões arquiteturais — ele é a especificação e o critério de avaliação ao mesmo tempo.

**Estado atual:** o repositório é um esqueleto do Spring Initializr sem modificações. O único código existente é `GestaoApplication.java` (ponto de entrada `@SpringBootApplication` vazio) e um teste placeholder (`contextLoads`). Ainda não existem entidades de domínio, controllers, migrations ou frontend. Nada do que está descrito abaixo em "Arquitetura-alvo" existe em código ainda — é o plano implícito nas dependências já escolhidas em `pom.xml` e nos requisitos do `PRD.md`.

> **Contexto adicional de avaliação:** este projeto não é só para passar nos requisitos do PRD — é material de entrevista. Cada decisão técnica não-trivial precisa ficar registrada e explicável (ver `docs/DECISIONS.md`). Trate esse arquivo com a mesma seriedade que o código.

---

## ⚙️ Fluxo de Desenvolvimento Obrigatório

**Nenhuma tarefa de código começa sem passar pelas etapas abaixo, nessa ordem.** Isso vale tanto para você (Claude Code) trabalhando sozinho quanto quando o usuário pedir uma feature nova, um bugfix ou um refactor. Se o usuário pedir para "pular direto pro código", lembre-o gentilmente desse fluxo antes de prosseguir — a menos que ele confirme explicitamente que quer pular uma etapa.

```
1. PLANEJAR           → especificar a tarefa antes de tocar em código
2. ESPECIFICAR        → criar specs/NNN-nome-da-tarefa.md
3. REGISTRAR DECISÕES → toda escolha não-trivial vai para docs/DECISIONS.md
4. IMPLEMENTAR (TDD)  → teste antes/junto da implementação
5. TESTES             → unitários + integração; cobertura ≥ 80% com sentido
6. E2E                → fluxos completos em e2e/, usados na pipeline
7. CODE REVIEW        → revisão funcional/qualidade antes de considerar pronto
8. SECURITY REVIEW    → revisão de segurança antes de considerar pronto
9. VERIFICAÇÃO FINAL  → checklist de definition of done
```

### 1. Planejar antes de desenvolver

Antes de escrever qualquer código de produção:
- Releia a seção relevante do `PRD.md`.
- Identifique se a tarefa afeta mais de um módulo Modulith (acadêmico vs. secundário) — se sim, isso é sinal de que precisa de mais de uma spec ou de uma spec com múltiplas frentes.
- Use o subagent `agent-organizer` (ou raciocínio próprio, se não estiver disponível) para quebrar a tarefa em passos concretos.
- Não avance para o passo 2 sem um plano de 3-8 passos claro.

### 2. Criar spec da tarefa

Toda tarefa (feature, bugfix não-trivial, refactor estrutural) precisa de um arquivo `specs/NNN-nome-curto.md`, criado **antes** de iniciar a implementação, usando o template em `specs/000-TEMPLATE.md`. Numeração sequencial (`001`, `002`, ...). A spec só é considerada "aprovada" quando as seções de escopo e critérios de aceite estão preenchidas — não como um placeholder.

Tarefas triviais (typo, formatação, ajuste de import) não precisam de spec — use o bom senso, mas errar para o lado de criar a spec quando em dúvida.

### 3. Registrar decisões técnicas

A skill `decision-log` (`.claude/skills/decision-log/SKILL.md`) governa isso — ela deve ser consultada sempre que uma decisão técnica não-trivial for tomada, mesmo dentro da implementação de uma spec já aprovada. O objetivo: `docs/DECISIONS.md` precisa deixar claro, ao final do projeto, **o que foi decisão deliberada do Pablo vs. o que foi default/sugestão aceita da IA sem alteração** — esse é o material que vai ser usado na entrevista técnica.

Regra de ouro: se existe mais de uma alternativa razoável (ex: lock otimista vs. pessimista, RabbitMQ vs. Kafka, estratégia de idempotência), é decisão não-trivial e precisa virar entrada em `DECISIONS.md`, com o usuário confirmando antes de a implementação prosseguir — não decida sozinho e documente depois.

### 4. Implementar com TDD

- Escreva o teste (ou pelo menos o esqueleto dele) antes ou junto da implementação, não depois.
- Siga a camada convencional definida em "Arquitetura-alvo" abaixo.
- Não deixe entidades JPA vazarem pela API — sempre DTOs na borda.

### 5. Testes unitários e de integração — cobertura ≥ 80% com sentido

- Meta de cobertura: **80% de linha/branch**, mas cobertura não é o objetivo em si — é consequência de testar bem as regras de negócio críticas do PRD §02 (vagas, status de matrícula, duplicidade, concorrência).
- Não escreva testes triviais só para inflar número (getter/setter, construtores gerados pelo Lombok). Prefira poucos testes que exercitam regra de negócio real a muitos testes que só cobrem linha.
- Configure JaCoCo no `pom.xml` com gate de build em 80% (`jacoco-maven-plugin`, `check` goal) assim que a primeira spec de domínio for implementada.
- Testes de integração usam Testcontainers (Postgres real), não H2 — o ambiente já é Postgres via `compose.yaml`, então o teste deve refletir isso.

### 6. Testes E2E na pasta `e2e/`

- Todo fluxo ponta a ponta relevante (ex: "aluno se matricula → confirma → vaga é consumida → evento é publicado → módulo secundário reage") vive em `e2e/`, na raiz do repositório — não dentro de `src/test/`.
- Esses testes são os que a pipeline de CI roda como gate de deploy/merge (ver `.github/workflows/`).
- Ferramenta: a definir na spec correspondente (ex: REST Assured contra a aplicação subida via Testcontainers, ou Playwright se o frontend entrar no fluxo).

### 7. Code review

Antes de considerar qualquer spec "concluída", invoque o subagent `code-reviewer` sobre o diff da tarefa. Ele deve avaliar: aderência à arquitetura-alvo, separação de camadas, nomeação, duplicação, tratamento de erros. Registre achados relevantes (aceitos ou descartados, com justificativa) em `docs/DECISIONS.md` se mudarem uma decisão já tomada.

### 8. Code review de segurança

Antes de considerar qualquer spec "concluída", invoque o subagent `security-auditor` sobre o diff da tarefa. Pontos de atenção constantes neste projeto: validação de entrada em todos os endpoints, autenticação/autorização (Spring Security + OAuth2 já estão no `pom.xml`), exposição de dados sensíveis em logs/erros, injeção (SQL/JPQL), condições de corrida na regra de vagas (é simultaneamente regra de negócio crítica *e* superfície de risco de segurança/consistência).

### 9. Verificação final antes de fechar a tarefa

Checklist mínimo (a skill `verification-before-completion`, se instalada via `obra/superpowers`, formaliza isso):
- [ ] Spec em `specs/NNN-*.md` reflete o que foi de fato implementado (atualize se divergiu).
- [ ] `docs/DECISIONS.md` atualizado com toda decisão não-trivial tomada durante a tarefa.
- [ ] Testes unitários/integração passando, cobertura ≥ 80% no módulo afetado.
- [ ] E2E relevante criado/atualizado em `e2e/`.
- [ ] `code-reviewer` executado e achados endereçados.
- [ ] `security-auditor` executado e achados endereçados.
- [ ] `./mvnw clean verify` passando localmente.

---

## 🤖 Subagents disponíveis neste projeto

Instalados em `.claude/agents/` (escopo de projeto, versionados no git). Fonte: [VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents).

| Subagent | Quando usar |
|---|---|
| `java-architect` | Decisões estruturais de pacote/módulo Java, padrões de design, revisão de arquitetura Spring Modulith |
| `spring-boot-engineer` | Implementação de controllers, services, configuração Spring Boot, integração JPA/Flyway |
| `angular-architect` | Estrutura do frontend, componentes, roteamento, consumo de API |
| `qa-expert` | Estratégia de testes, cenários de teste para regras de negócio críticas |
| `test-automator` | Escrita de testes automatizados (unit/integration/e2e), configuração de Testcontainers |
| `code-reviewer` | Etapa 7 do fluxo — revisão funcional/qualidade de todo diff antes de fechar tarefa |
| `security-auditor` | Etapa 8 do fluxo — revisão de segurança de todo diff antes de fechar tarefa |
| `architect-reviewer` | Revisões arquiteturais maiores (ex: antes de adicionar mensageria, antes de dividir módulos) |
| `technical-writer` | README final, documento arquitetural curto exigido pelo PRD §05 |
| `agent-organizer` | Etapa 1 — quebrar tarefas complexas em passos/subagentes |

## 🧩 Skills disponíveis neste projeto

Instaladas em `.claude/skills/` (escopo de projeto). Fonte: [obra/superpowers](https://github.com/obra/superpowers) via `skills.sh`, mais a skill customizada deste repositório.

| Skill | Papel no fluxo |
|---|---|
| `brainstorming` | Etapa 1 — explorar alternativas antes de fechar o plano |
| `writing-plans` | Etapa 1/2 — transformar o plano em spec estruturada |
| `executing-plans` | Etapa 4 — seguir a spec de forma disciplinada durante a implementação |
| `test-driven-development` | Etapa 4/5 — disciplina de escrever teste antes/junto do código |
| `requesting-code-review` / `receiving-code-review` | Etapa 7 — protocolo de como pedir e responder a revisões |
| `verification-before-completion` | Etapa 9 — checklist final antes de considerar a tarefa pronta |
| `decision-log` (customizada, ver `.claude/skills/decision-log/SKILL.md`) | Etapa 3 — governa como e quando registrar decisões em `docs/DECISIONS.md` |

---

## Build, execução e testes

Use o Maven wrapper (não é necessário Maven instalado localmente):

```
./mvnw spring-boot:run       # executa a aplicação (sobe postgres/redis via compose.yaml automaticamente)
./mvnw test                  # executa todos os testes
./mvnw test -Dtest=NomeDaClasse#metodo   # executa um único método de teste
./mvnw clean verify          # build completo incluindo testes
./mvnw clean package         # gera o jar
```

O `spring-boot-docker-compose` está no classpath de runtime, então, ao rodar localmente, a aplicação sobe automaticamente os serviços do `compose.yaml` (Postgres + Redis) caso detecte que ainda não estão rodando — não é necessário `docker compose up` manual para desenvolvimento local. As portas de ambos os serviços são efêmeras (`'5432'`, `'6379'` mapeiam para portas aleatórias no host); o Spring Boot injeta as portas reais automaticamente via essa integração.

Conforme o PRD, o Docker Compose da solução final também precisa subir a própria aplicação e um message broker (RabbitMQ/Kafka) — o `compose.yaml` atual só tem `postgres` e `redis` e precisará crescer para atender a esse requisito.

## Stack

- Java 21, Spring Boot 3.5.16 (o BOM do parent controla a maioria das versões de dependências)
- Web: `spring-boot-starter-web`
- Persistência: `spring-boot-starter-data-jpa` + driver PostgreSQL + Flyway (`flyway-core`, `flyway-database-postgresql`) — migrations devem ficar em `src/main/resources/db/migration` (nenhuma existe ainda)
- Sessões: `spring-session-data-redis`
- Segurança: `spring-boot-starter-security` + `spring-boot-starter-oauth2-client` (presentes mas ainda não configurados — não há `SecurityConfig` ainda)
- Documentação de API: `springdoc-openapi-starter-webmvc-ui` 2.8.16 → o Swagger UI ficará disponível em `/swagger-ui.html` assim que existirem controllers
- Modularidade: `spring-modulith-starter-core` / `-jpa` / `-runtime` / `-test` (gerenciados pelo BOM, versão `1.4.12` — ver a propriedade `spring-modulith.version` em `pom.xml`)
- Lombok + `spring-boot-configuration-processor` configurados como annotation processors no plugin do compilador
- Testes: `spring-boot-starter-test`, `spring-modulith-starter-test`, `spring-security-test`

Pacote base: `br.com.desafio.tecnico.gestao`.

## Arquitetura-alvo (a partir dos requisitos do PRD)

As dependências já escolhidas embutem uma decisão arquitetural: a intenção é um **monólito modular** usando Spring Modulith, não uma solução em camadas artesanal nem uma divisão prematura em microsserviços. Na prática, isso implica:

- Organizar o código em pacotes de nível superior sob o pacote base, um por módulo/contexto delimitado (o Spring Modulith trata cada subpacote direto do pacote da aplicação como um módulo e reforça os limites entre módulos em tempo de teste via `ApplicationModules.of(GestaoApplication.class).verify()`).
- O PRD exige separação clara entre o **contexto acadêmico** (Aluno, Curso, Disciplina, Turma, Matrícula) e um **contexto secundário** (notificações, auditoria ou relatórios) que reage a eventos de domínio publicados pelo contexto acadêmico — isso mapeia diretamente para módulos Modulith separados, com o contexto secundário consumindo eventos em vez de ser chamado de forma síncrona.
- Dentro do módulo acadêmico, manter a camada convencional que o PRD pede explicitamente: controller → service/application → domain/model → repository/persistence, com DTOs na borda (não deixar entidades JPA vazarem pela API REST).
- Eventos de domínio a publicar: `MatriculaCriada`, `MatriculaConfirmada`, `MatriculaCancelada`. Devem ser consumidos de forma assíncrona pelo módulo/serviço secundário — via mecanismo de publicação de eventos do Spring Modulith e/ou um broker externo (RabbitMQ/Kafka), conforme o requisito de mensageria abaixo.
- Mensageria: o PRD exige mensageria assíncrona real (RabbitMQ, Kafka ou equivalente) com um consumidor em módulo ou serviço separado — isso ainda não está no `pom.xml` e precisará ser adicionado.

> Nota: a escolha entre publicar eventos apenas via Spring Modulith (intra-processo) vs. via broker externo (inter-processo) é exatamente o tipo de decisão que deve passar pela etapa 3 (registrar em `docs/DECISIONS.md`) antes de ser implementada — o PRD pede explicitamente comunicação assíncrona via broker real, então isso não é opcional, mas a forma exata de integrar Modulith + broker é.

### Regras de negócio centrais a garantir (PRD §02)

- Um aluno só pode se matricular em turmas abertas.
- Uma turma possui limite de vagas; confirmar uma matrícula consome uma vaga, cancelar uma matrícula confirmada libera a vaga.
- Um aluno não pode se matricular duas vezes na mesma turma.
- Matrícula possui status: `PENDENTE`, `CONFIRMADA`, `CANCELADA`.
- A consistência do limite de vagas sob matrícula concorrente é destacada como ponto crítico de avaliação — as perguntas de entrevista do PRD questionam especificamente o que acontece quando duas requisições disputam a última vaga, então a estratégia de bloqueio/consistência usada (lock pessimista, lock otimista + retry, constraint no banco, etc.) deve ser deliberada e explicável, não incidental. **Essa decisão específica é candidata natural a uma entrada detalhada em `docs/DECISIONS.md`.**
- Deve haver consulta de matrículas por aluno e por turma.

### Expectativas não funcionais (PRD §04–05)

- Observabilidade: logs estruturados, correlation ID ou trace ID, health checks, métricas básicas.
- Tratamento padronizado de erros/validação em toda a API REST.
- O README (a ser escrito) deve cobrir: instruções de execução local, instruções de execução via Docker Compose, como rodar os testes, tecnologias escolhidas, como a regra de limite de vagas é protegida, como a concorrência é tratada, como os eventos de domínio são publicados/consumidos, como falhas de mensageria são tratadas, mecanismos de observabilidade e uso/divulgação de ferramentas de IA.
- Espera-se também um documento arquitetural curto cobrindo decisões, trade-offs, riscos conhecidos e caminho de evolução, junto com o README — **esse documento pode (e deve) puxar diretamente de `docs/DECISIONS.md`**, que é mais granular e serve de fonte primária.

## Observações

- Os blocos vazios `<license/>`, `<developers/>` etc. no `pom.xml` são boilerplate intencional do Spring Initializr para evitar que esses elementos sejam herdados do parent POM — não é algo a preencher.
- Ainda não existe frontend; o PRD exige um em Angular, JS/TS puro, ou framework web equivalente, estruturado em componentes/telas com consumo organizado da API e tratamento de erros.
- Este `CLAUDE.md` é o documento de processo. `PRD.md` é a especificação funcional/critério de avaliação. `docs/DECISIONS.md` é o log de decisões técnicas. `specs/` são os planos por tarefa. Não duplique conteúdo entre eles — cada um tem uma função específica.
