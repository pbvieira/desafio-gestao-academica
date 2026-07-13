#!/usr/bin/env bash
# e2e/administracao-papel-flow.sh
# specs/010: ADMIN reatribui o papel de um usuário e confirma via novo login que o
# token reflete o novo papel. Requer a stack completa no ar (compose + backend em :8080
# + Keycloak em :8081) e as credenciais de teste do README.
set -euo pipefail

API="${API_BASE_URL:-http://localhost:8080}"
KEYCLOAK="${KEYCLOAK_BASE_URL:-http://localhost:8081}"
REALM="gestao"

token() {
  curl -s -X POST "$KEYCLOAK/realms/$REALM/protocol/openid-connect/token" \
    -d "client_id=gestao-frontend" -d "grant_type=password" \
    -d "username=$1" -d "password=$2" | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])"
}

echo "1) Login como admin.teste"
ADMIN_TOKEN=$(token admin.teste admin123)

echo "2) Listar usuarios via GET /api/admin/usuarios"
USUARIOS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$API/api/admin/usuarios")
SECRETARIA_ID=$(echo "$USUARIOS" | python3 -c "
import sys, json
for u in json.load(sys.stdin):
    if u['username'] == 'secretaria.teste':
        print(u['id']); break
")
if [ -z "$SECRETARIA_ID" ]; then
  echo "FALHA: secretaria.teste nao encontrada na listagem"; exit 1
fi
echo "OK - secretaria.teste id=$SECRETARIA_ID"

echo "3) Reatribuir papel de secretaria.teste para ADMIN"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"papel":"ADMIN"}' "$API/api/admin/usuarios/$SECRETARIA_ID/papel")
if [ "$STATUS" != "204" ]; then
  echo "FALHA: PATCH retornou $STATUS, esperado 204"; exit 1
fi
echo "OK - papel reatribuido (204)"

echo "4) Novo login de secretaria.teste deve trazer o papel ADMIN no token"
NOVO_TOKEN=$(token secretaria.teste secretaria123)
PAPEIS=$(echo "$NOVO_TOKEN" | python3 -c "
import sys, json, base64
token = sys.stdin.read().strip()
payload = token.split('.')[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))['realm_access']['roles'])
")
if [[ "$PAPEIS" != *"ADMIN"* ]]; then
  echo "FALHA: token de secretaria.teste nao contem ADMIN apos reatribuicao: $PAPEIS"; exit 1
fi
echo "OK - novo token de secretaria.teste contem ADMIN"

echo "5) Reverter: devolver secretaria.teste para o papel SECRETARIA (deixa o realm como estava)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"papel":"SECRETARIA"}' "$API/api/admin/usuarios/$SECRETARIA_ID/papel")
if [ "$STATUS" != "204" ]; then
  echo "FALHA: reversao retornou $STATUS, esperado 204"; exit 1
fi
echo "OK - papel revertido para SECRETARIA"

echo "=================================="
echo "Todas as validacoes passaram."
