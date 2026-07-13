#!/usr/bin/env bash
set -uo pipefail

# specs/010-administracao-usuarios-papeis.md: ADMIN reatribui o papel de um usuario e
# confirma via novo login que o token reflete o novo papel. Segue a mesma convencao dos
# demais scripts de e2e/ (matricula-flow.sh, smoke-test.sh): set -uo pipefail (sem -e) +
# contador de falhas que acumula e reporta todas ao final, cd para a raiz do repo, e
# .env carregado via source (nao defaults hardcoded que so coincidem por acaso).
#
# Uso: bash e2e/administracao-papel-flow.sh
# Pressupoe: docker compose (Keycloak com o realm gestao importado, incluindo os
# usuarios aluno.teste/secretaria.teste/admin.teste) e a aplicacao (./mvnw spring-boot:run)
# ja de pe - o CI sobe os dois antes de chamar este script (mesmo pressuposto do
# smoke-test.sh/matricula-flow.sh).

cd "$(dirname "$0")/.."

if [ -f .env ]; then
	set -a
	# shellcheck disable=SC1091
	source .env
	set +a
fi

KEYCLOAK_HTTP_PORT="${KEYCLOAK_HTTP_PORT:-8081}"
APP_PORT="${APP_PORT:-8080}"
KC="http://localhost:${KEYCLOAK_HTTP_PORT}"
APP="http://localhost:${APP_PORT}"
REALM="gestao"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

falhas=0

log_ok() { echo "[OK]     $1"; }
log_falha() {
	echo "[FALHA]  $1"
	falhas=$((falhas + 1))
}

obter_token() {
	local username="$1" password="$2" arquivo_saida="$3"
	curl -s -X POST "${KC}/realms/${REALM}/protocol/openid-connect/token" \
		-d "grant_type=password" -d "client_id=gestao-frontend" \
		-d "username=${username}" -d "password=${password}" -d "scope=openid" \
		-o "${arquivo_saida}"
}

extrair_token() {
	python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get('access_token',''))" "$1"
}

extrair_campo() {
	python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get(sys.argv[2],''))" "$1" "$2"
}

# Decodifica os papeis (realm_access.roles) do JWT (recebido como argumento, ja
# extraido de access_token) sem validar assinatura - so para confirmar que o novo login
# de secretaria.teste reflete a reatribuicao feita no Step 3.
extrair_papeis() {
	python3 -c "
import json, sys, base64
token = sys.argv[1]
payload = token.split('.')[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload)).get('realm_access', {}).get('roles', []))
" "$1"
}

chamar() {
	local metodo="$1" url="$2" token="$3" corpo="${4:-}" arquivo_saida="$5"
	if [ -n "$corpo" ]; then
		curl -s -o "$arquivo_saida" -w "%{http_code}" -X "$metodo" "${APP}${url}" \
			-H "Authorization: Bearer ${token}" -H "Content-Type: application/json" -d "$corpo"
	else
		curl -s -o "$arquivo_saida" -w "%{http_code}" -X "$metodo" "${APP}${url}" \
			-H "Authorization: Bearer ${token}"
	fi
}

esperar_status() {
	local descricao="$1" esperado="$2" obtido="$3"
	if [ "$obtido" = "$esperado" ]; then
		log_ok "$descricao (HTTP $obtido)"
	else
		log_falha "$descricao (esperado HTTP $esperado, obtido HTTP $obtido)"
	fi
}

# Achado de code review (task-reviewer): se o Step 3 (reatribuicao para ADMIN) tiver
# sucesso e qualquer etapa seguinte falhar (ou o script sair por outro motivo), o realm
# nao pode ficar com secretaria.teste permanentemente como ADMIN. A reversao so e
# armada (via este trap) depois que o Step 3 confirma sucesso - antes disso nao ha nada
# para reverter.
PAPEL_REATRIBUIDO=0
reverter_papel_secretaria() {
	if [ "$PAPEL_REATRIBUIDO" -eq 1 ]; then
		local status
		status=$(chamar PATCH "/api/admin/usuarios/${SECRETARIA_ID}/papel" "$ADMIN_TOKEN" \
			'{"papel":"SECRETARIA"}' "${TMP_DIR}/reversao.json")
		if [ "$status" = "204" ]; then
			log_ok "papel de secretaria.teste revertido para SECRETARIA (trap de saida)"
		else
			log_falha "reversao de secretaria.teste via trap retornou $status, esperado 204 - REVERTA MANUALMENTE"
		fi
	fi
}
trap 'reverter_papel_secretaria; rm -rf "$TMP_DIR"' EXIT

echo "== E2E: administracao de usuarios/papeis (ADMIN reatribui papel de secretaria.teste) =="

echo "1) Login como admin.teste"
obter_token "admin.teste" "admin123" "${TMP_DIR}/token_admin.json"
ADMIN_TOKEN="$(extrair_token "${TMP_DIR}/token_admin.json")"
if [ -z "$ADMIN_TOKEN" ]; then
	log_falha "obter token de admin.teste"
	echo "$falhas falha(s) - abortando (sem token não há como continuar)."
	exit 1
fi
log_ok "token de admin.teste obtido do Keycloak real"

echo "2) Listar usuarios via GET /api/admin/usuarios"
CODIGO=$(chamar GET "/api/admin/usuarios" "$ADMIN_TOKEN" "" "${TMP_DIR}/usuarios.json")
esperar_status "listar usuarios" 200 "$CODIGO"
SECRETARIA_ID=$(python3 -c "
import json, sys
usuarios = json.load(open(sys.argv[1]))
encontrados = [u['id'] for u in usuarios if u.get('username') == 'secretaria.teste']
print(encontrados[0] if encontrados else '')
" "${TMP_DIR}/usuarios.json")
if [ -z "$SECRETARIA_ID" ]; then
	log_falha "localizar secretaria.teste na listagem"
	echo "$falhas falha(s) - abortando (sem o id não há como continuar)."
	exit 1
fi
log_ok "secretaria.teste localizada (id=$SECRETARIA_ID)"

echo "3) Reatribuir papel de secretaria.teste para ADMIN"
CODIGO=$(chamar PATCH "/api/admin/usuarios/${SECRETARIA_ID}/papel" "$ADMIN_TOKEN" \
	'{"papel":"ADMIN"}' "${TMP_DIR}/reatribuicao.json")
if [ "$CODIGO" = "204" ]; then
	log_ok "papel reatribuido para ADMIN (204)"
	PAPEL_REATRIBUIDO=1
else
	log_falha "reatribuicao de papel retornou $CODIGO, esperado 204"
fi

echo "4) Novo login de secretaria.teste deve trazer o papel ADMIN no token"
obter_token "secretaria.teste" "secretaria123" "${TMP_DIR}/token_secretaria.json"
NOVO_TOKEN="$(extrair_token "${TMP_DIR}/token_secretaria.json")"
if [ -z "$NOVO_TOKEN" ]; then
	log_falha "obter novo token de secretaria.teste apos reatribuicao"
else
	PAPEIS=$(extrair_papeis "$NOVO_TOKEN")
	if [[ "$PAPEIS" == *"ADMIN"* ]]; then
		log_ok "novo token de secretaria.teste contem ADMIN"
	else
		log_falha "token de secretaria.teste nao contem ADMIN apos reatribuicao: $PAPEIS"
	fi
fi

echo "5) Reverter: devolver secretaria.teste para o papel SECRETARIA (deixa o realm como estava)"
CODIGO=$(chamar PATCH "/api/admin/usuarios/${SECRETARIA_ID}/papel" "$ADMIN_TOKEN" \
	'{"papel":"SECRETARIA"}' "${TMP_DIR}/reversao_final.json")
if [ "$CODIGO" = "204" ]; then
	log_ok "papel revertido para SECRETARIA"
	PAPEL_REATRIBUIDO=0
else
	log_falha "reversao (Step 5) retornou $CODIGO, esperado 204 - trap de saida ainda tentara reverter"
fi

echo "=================================="
if [ "$falhas" -eq 0 ]; then
	echo "Fluxo de administracao de usuarios/papeis completo, sem falhas."
	exit 0
else
	echo "$falhas etapa(s) com falha."
	exit 1
fi
