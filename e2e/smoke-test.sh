#!/usr/bin/env bash
set -uo pipefail

# Smoke test de infraestrutura (specs/004-fundacoes-transversais.md, secoes 4.7/7).
# Valida que todos os servicos do compose (incl. profile observability) e a propria
# aplicacao estao de pe e respondendo - nao testa nenhuma regra de negocio (nao ha
# nenhuma implementada ainda). E' o primeiro item real que o workflow de CI executa,
# depois de "./mvnw clean verify".
#
# Uso: bash e2e/smoke-test.sh
# Pressupoe: docker compose --profile observability já está de pé, e a aplicação rodando
# (./mvnw spring-boot:run) - o próprio CI sobe os dois antes de chamar este script.

cd "$(dirname "$0")/.."

if [ -f .env ]; then
	set -a
	# shellcheck disable=SC1091
	source .env
	set +a
fi

POSTGRES_USER="${POSTGRES_USER:-myuser}"
RABBITMQ_MANAGEMENT_PORT="${RABBITMQ_MANAGEMENT_PORT:-15672}"
RABBITMQ_DEFAULT_USER="${RABBITMQ_DEFAULT_USER:-guest}"
RABBITMQ_DEFAULT_PASS="${RABBITMQ_DEFAULT_PASS:?RABBITMQ_DEFAULT_PASS nao definido (copie .env.example para .env)}"
KEYCLOAK_HTTP_PORT="${KEYCLOAK_HTTP_PORT:-8081}"
PROMETHEUS_PORT="${PROMETHEUS_PORT:-9090}"
JAEGER_UI_PORT="${JAEGER_UI_PORT:-16686}"
LOKI_PORT="${LOKI_PORT:-3100}"
APP_PORT="${APP_PORT:-8080}"

falhas=0

verificar() {
	local nome="$1"
	shift
	if "$@" >/dev/null 2>&1; then
		echo "[OK]     $nome"
	else
		echo "[FALHA]  $nome"
		falhas=$((falhas + 1))
	fi
}

echo "== Smoke test de infraestrutura =="

verificar "Postgres (pg_isready)" \
	docker compose exec -T postgres pg_isready -U "$POSTGRES_USER"

verificar "RabbitMQ (management API)" \
	curl -sf -u "${RABBITMQ_DEFAULT_USER}:${RABBITMQ_DEFAULT_PASS}" \
	"http://localhost:${RABBITMQ_MANAGEMENT_PORT}/api/overview"

verificar "Keycloak (realm gestao bem-formado)" \
	bash -c "curl -sf http://localhost:${KEYCLOAK_HTTP_PORT}/realms/gestao/.well-known/openid-configuration | grep -q issuer"

verificar "Prometheus (/-/ready)" \
	curl -sf "http://localhost:${PROMETHEUS_PORT}/-/ready"

verificar "Jaeger (UI)" \
	curl -sf -o /dev/null "http://localhost:${JAEGER_UI_PORT}/"

verificar "Loki (/ready)" \
	curl -sf "http://localhost:${LOKI_PORT}/ready"

verificar "Aplicacao (/actuator/health = UP)" \
	bash -c "curl -sf http://localhost:${APP_PORT}/actuator/health | grep -q '\"status\":\"UP\"'"

echo "=================================="
if [ "$falhas" -eq 0 ]; then
	echo "Todos os servicos respondendo."
	exit 0
else
	echo "$falhas servico(s) com falha."
	exit 1
fi
