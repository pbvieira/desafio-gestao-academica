#!/bin/bash
set -euo pipefail

# Cria uma role dedicada para o Keycloak, dona do banco "keycloak", em vez de reaproveitar
# o superusuário da aplicação (POSTGRES_USER) — ver D004 em docs/DECISIONS.md (achado de
# security review: reuso de credencial quebrava a segregação de privilégio pretendida).
# KEYCLOAK_DB_USER e KEYCLOAK_DB_PASSWORD vêm do ambiente do próprio container postgres
# (definidos em compose.yaml a partir do .env).
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE "${KEYCLOAK_DB_USER}" WITH LOGIN PASSWORD '${KEYCLOAK_DB_PASSWORD}';
    CREATE DATABASE keycloak OWNER "${KEYCLOAK_DB_USER}";
EOSQL
