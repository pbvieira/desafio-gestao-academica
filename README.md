# Gestão de Matrículas Acadêmicas

Desafio técnico — Desenvolvedor(a) Sênior Full Stack, Tribe Lyceum – Techne. Sistema de
gestão de matrículas acadêmicas (Spring Boot + Angular). Ver `PRD.md` para a especificação
completa e `docs/DECISIONS.md` para o histórico de decisões técnicas.

> **Status:** Fase 1 (infraestrutura, observabilidade, segurança e fundações transversais)
> concluída. Nenhuma entidade de domínio (Aluno, Curso, Turma, Matrícula) implementada
> ainda — ver `specs/` para o plano por fase. Este README será completado ao longo do
> desenvolvimento das próximas fases.

## Como rodar localmente

Pré-requisitos: Java 21, Docker e Docker Compose.

1. Copie o arquivo de variáveis de ambiente de exemplo e ajuste os valores:
   ```
   cp .env.example .env
   ```
2. Suba a infraestrutura (Postgres, Redis, RabbitMQ, Keycloak):
   ```
   docker compose up -d
   ```
   Para incluir também a stack de observabilidade (Prometheus, Grafana, Jaeger, Loki):
   ```
   docker compose --profile observability up -d
   ```
3. Rode a aplicação (fora do compose — ver `docs/DECISIONS.md`, D003):
   ```
   ./mvnw spring-boot:run
   ```

A aplicação sobe em `http://localhost:8080`. Swagger UI em
`http://localhost:8080/swagger-ui.html`.

## Como rodar os testes

```
./mvnw clean verify
```

Roda os testes automatizados e o gate de cobertura JaCoCo (≥ 80% de linha/branch,
exclusões documentadas em `docs/DECISIONS.md`, D014).

## Tecnologias

Java 21, Spring Boot 3.5, Spring Modulith, Spring Security (OAuth2 Resource Server),
PostgreSQL + Flyway, Redis, RabbitMQ, Keycloak, Prometheus + Grafana + Jaeger + Loki
(observabilidade), JaCoCo. Ver `pom.xml` e `CLAUDE.md` para a lista completa.

## Documentação

- `docs/DECISIONS.md` — log de decisões técnicas (o que foi decisão deliberada vs. default
  aceito da IA).
- `specs/` — spec de cada fase do desenvolvimento.
- `CLAUDE.md` — guia de arquitetura e fluxo de trabalho do projeto.

## Uso de IA

Este projeto foi desenvolvido com apoio de Claude Code em todas as fases (planejamento,
specs, implementação, revisão de código e segurança). Toda decisão técnica não-trivial —
inclusive quais foram sugestões da IA aceitas sem alteração vs. decisões ativas do autor —
está registrada em `docs/DECISIONS.md`, com a origem de cada uma classificada
explicitamente. Achados de `code-reviewer`/`security-auditor` (também via IA) que geraram
mudança de implementação estão documentados nas specs correspondentes em `specs/`.
