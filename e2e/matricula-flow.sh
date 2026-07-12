#!/usr/bin/env bash
set -uo pipefail

# Fluxo de negocio real de Matricula (specs/006-matricula.md, secao 7/D030) - primeiro
# teste de dominio real em e2e/ (o smoke-test.sh so testa infraestrutura). Ao contrario
# dos testes de integracao (MockMvc + JWT simulado via SecurityMockMvcRequestPostProcessors),
# este script busca tokens REAIS do Keycloak do compose e fala HTTP real com a aplicacao
# rodando - valida a cadeia OAuth2 Resource Server inteira (issuer/JWKS), nao so o
# PermissionEvaluator.
#
# Uso: bash e2e/matricula-flow.sh
# Pressupoe: docker compose (Keycloak com o realm gestao importado, incluindo os
# usuarios aluno.teste/secretaria.teste) e a aplicacao (./mvnw spring-boot:run) ja
# de pe - o CI sobe os dois antes de chamar este script (mesmo pressuposto do
# smoke-test.sh).

cd "$(dirname "$0")/.."

if [ -f .env ]; then
	set -a
	# shellcheck disable=SC1091
	source .env
	set +a
fi

KEYCLOAK_HTTP_PORT="${KEYCLOAK_HTTP_PORT:-8081}"
APP_PORT="${APP_PORT:-8080}"
POSTGRES_USER="${POSTGRES_USER:-myuser}"
POSTGRES_DB="${POSTGRES_DB:-mydatabase}"
KC="http://localhost:${KEYCLOAK_HTTP_PORT}"
APP="http://localhost:${APP_PORT}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

falhas=0

log_ok() { echo "[OK]     $1"; }
log_falha() {
	echo "[FALHA]  $1"
	falhas=$((falhas + 1))
}

# Client publico (directAccessGrantsEnabled=true) do realm gestao - mesmo usado pela
# validacao manual de RBAC/ABAC desta fase, ver docker/keycloak/import/gestao-realm.json.
obter_token() {
	local username="$1" password="$2" arquivo_saida="$3"
	curl -s -X POST "${KC}/realms/gestao/protocol/openid-connect/token" \
		-d "grant_type=password" -d "client_id=gestao-frontend" \
		-d "username=${username}" -d "password=${password}" -d "scope=openid" \
		-o "${arquivo_saida}"
}

extrair_token() {
	python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get('access_token',''))" "$1"
}

# Decodifica o claim "sub" do JWT sem validar assinatura - so para popular o
# keycloakSubjectId do Aluno de teste com o subject REAL do aluno.teste (achado desta
# fase, D030: sem isso, ABAC nunca teria como casar dono real com o Aluno cadastrado).
extrair_subject() {
	python3 -c "
import json, sys, base64
token = open(sys.argv[1]).read().strip()
payload = token.split('.')[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload)).get('sub', ''))
" "$1"
}

extrair_campo() {
	python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get(sys.argv[2],''))" "$1" "$2"
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

# specs/007-mensageria-rabbitmq.md, seção 4.6/D035: consulta direta ao Postgres do
# compose (mesmo container que a aplicação usa) para confirmar que o consumidor real
# (RabbitMQ, não mais @ApplicationModuleListener interno da Fase 3) processou os
# eventos de domínio e gravou o registro de idempotência - sem isso, o e2e só provaria
# a API REST, não a mensageria assíncrona de verdade.
aguardar_evento_processado() {
	local descricao="$1" minimo_esperado="$2" tentativas=15
	for _ in $(seq 1 "$tentativas"); do
		local contagem
		contagem=$(docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
			"SELECT COUNT(*) FROM evento_processado" 2>/dev/null | tr -d '[:space:]')
		if [ -n "$contagem" ] && [ "$contagem" -ge "$minimo_esperado" ]; then
			log_ok "$descricao (evento_processado tem $contagem registro(s))"
			return 0
		fi
		sleep 1
	done
	log_falha "$descricao (evento_processado não chegou a $minimo_esperado registro(s) em ${tentativas}s)"
}

echo "== E2E: fluxo de Matrícula (criar -> confirmar -> duplicidade -> cancelar -> vaga liberada) =="

obter_token "aluno.teste" "aluno123" "${TMP_DIR}/token_aluno.json"
obter_token "secretaria.teste" "secretaria123" "${TMP_DIR}/token_secretaria.json"
TOKEN_ALUNO="$(extrair_token "${TMP_DIR}/token_aluno.json")"
TOKEN_SECRETARIA="$(extrair_token "${TMP_DIR}/token_secretaria.json")"
SUBJECT_ALUNO="$(extrair_subject "${TMP_DIR}/token_aluno.json")"

if [ -z "$TOKEN_ALUNO" ] || [ -z "$TOKEN_SECRETARIA" ]; then
	log_falha "obter tokens do Keycloak (aluno.teste/secretaria.teste)"
	echo "$falhas falha(s) - abortando (sem token não há como continuar)."
	exit 1
fi
log_ok "tokens obtidos do Keycloak real"

# "E2E-C-"/"E2E-D-"/"E2E-T-" (6 caracteres) + este sufixo precisa caber no limite de 20
# caracteres do campo código (CursoRequest/DisciplinaRequest/TurmaRequest) -
# "date +%s%N" (nanossegundos, ate 19 digitos) estourava esse limite e causava 400 em
# vez de 201 (mesmo achado do MatriculaConcorrenciaIntegrationTest.java).
SUFIXO="$(date +%s)"

CODIGO=$(chamar POST "/api/cursos" "$TOKEN_SECRETARIA" \
	"{\"codigo\":\"E2E-C-${SUFIXO}\",\"nome\":\"Curso E2E Matrícula\"}" "${TMP_DIR}/curso.json")
esperar_status "criar curso" 201 "$CODIGO"
CURSO_ID="$(extrair_campo "${TMP_DIR}/curso.json" id)"

CODIGO=$(chamar POST "/api/disciplinas" "$TOKEN_SECRETARIA" \
	"{\"codigo\":\"E2E-D-${SUFIXO}\",\"nome\":\"Disciplina E2E Matrícula\"}" "${TMP_DIR}/disciplina.json")
esperar_status "criar disciplina" 201 "$CODIGO"
DISCIPLINA_ID="$(extrair_campo "${TMP_DIR}/disciplina.json" id)"

CODIGO=$(chamar POST "/api/cursos/${CURSO_ID}/disciplinas/${DISCIPLINA_ID}" "$TOKEN_SECRETARIA" "" "${TMP_DIR}/vinculo.json")
esperar_status "vincular disciplina ao curso" 200 "$CODIGO"

CODIGO=$(chamar POST "/api/turmas" "$TOKEN_SECRETARIA" \
	"{\"codigo\":\"E2E-T-${SUFIXO}\",\"cursoId\":${CURSO_ID},\"disciplinaId\":${DISCIPLINA_ID},\"limiteVagas\":1}" \
	"${TMP_DIR}/turma.json")
esperar_status "criar turma (1 vaga)" 201 "$CODIGO"
TURMA_ID="$(extrair_campo "${TMP_DIR}/turma.json" id)"

# Idempotente por design: o subject do aluno.teste no Keycloak é fixo (não dá para
# randomizar), então rodar este script mais de uma vez contra o mesmo banco (ex:
# execução manual local, fora do CI que sempre começa com banco limpo) esbarra na
# constraint de unicidade de keycloakSubjectId (D030) - 409 nesse caso não é falha,
# significa que o Aluno de um run anterior já existe; reaproveita o mesmo id.
CODIGO=$(chamar POST "/api/alunos" "$TOKEN_SECRETARIA" \
	"{\"nome\":\"Aluno E2E\",\"email\":\"aluno.e2e.${SUFIXO}@example.com\",\"keycloakSubjectId\":\"${SUBJECT_ALUNO}\"}" \
	"${TMP_DIR}/aluno.json")
if [ "$CODIGO" = "409" ]; then
	log_ok "aluno já vinculado ao subject real do Keycloak (execução anterior) - reaproveitando"
	chamar GET "/api/alunos" "$TOKEN_SECRETARIA" "" "${TMP_DIR}/alunos.json" >/dev/null
	ALUNO_ID=$(python3 -c "
import json, sys
alunos = json.load(open(sys.argv[1]))
encontrados = [a['id'] for a in alunos if a.get('keycloakSubjectId') == sys.argv[2]]
print(encontrados[0] if encontrados else '')
" "${TMP_DIR}/alunos.json" "$SUBJECT_ALUNO")
	if [ -z "$ALUNO_ID" ]; then
		log_falha "localizar aluno existente vinculado ao subject real do Keycloak"
	fi
else
	esperar_status "cadastrar aluno vinculado ao subject real do Keycloak" 201 "$CODIGO"
	ALUNO_ID="$(extrair_campo "${TMP_DIR}/aluno.json" id)"
fi

CODIGO=$(chamar POST "/api/matriculas" "$TOKEN_ALUNO" \
	"{\"alunoId\":${ALUNO_ID},\"turmaId\":${TURMA_ID}}" "${TMP_DIR}/matricula.json")
esperar_status "aluno cria a própria matrícula (PENDENTE)" 201 "$CODIGO"
MATRICULA_ID="$(extrair_campo "${TMP_DIR}/matricula.json" id)"

CODIGO=$(chamar POST "/api/matriculas" "$TOKEN_ALUNO" \
	"{\"alunoId\":${ALUNO_ID},\"turmaId\":${TURMA_ID}}" "${TMP_DIR}/duplicidade.json")
esperar_status "duplicidade (mesma matrícula ativa) deve falhar" 409 "$CODIGO"

CODIGO=$(chamar POST "/api/matriculas/${MATRICULA_ID}/confirmar" "$TOKEN_SECRETARIA" "" "${TMP_DIR}/confirmar.json")
esperar_status "secretaria confirma a matrícula (consome a vaga)" 200 "$CODIGO"

CODIGO=$(chamar GET "/api/turmas/${TURMA_ID}" "$TOKEN_SECRETARIA" "" "${TMP_DIR}/turma_pos_confirmar.json")
VAGAS_OCUPADAS="$(extrair_campo "${TMP_DIR}/turma_pos_confirmar.json" vagasOcupadas)"
if [ "$VAGAS_OCUPADAS" = "1" ]; then
	log_ok "vaga consumida (vagasOcupadas=1)"
else
	log_falha "vaga consumida (esperado vagasOcupadas=1, obtido '${VAGAS_OCUPADAS}')"
fi

CODIGO=$(chamar POST "/api/matriculas/${MATRICULA_ID}/cancelar" "$TOKEN_ALUNO" "" "${TMP_DIR}/cancelar.json")
esperar_status "aluno cancela a própria matrícula confirmada" 200 "$CODIGO"

CODIGO=$(chamar GET "/api/turmas/${TURMA_ID}" "$TOKEN_SECRETARIA" "" "${TMP_DIR}/turma_pos_cancelar.json")
VAGAS_OCUPADAS="$(extrair_campo "${TMP_DIR}/turma_pos_cancelar.json" vagasOcupadas)"
if [ "$VAGAS_OCUPADAS" = "0" ]; then
	log_ok "vaga liberada (vagasOcupadas=0)"
else
	log_falha "vaga liberada (esperado vagasOcupadas=0, obtido '${VAGAS_OCUPADAS}')"
fi

# criar + confirmar + cancelar publicam 3 eventos (MatriculaCriada/Confirmada/Cancelada);
# execuções anteriores do script (idempotente, ver acima) podem já ter deixado outros
# registros na tabela - por isso o mínimo é ">= 3", não "== 3".
aguardar_evento_processado "consumidor RabbitMQ processou os eventos desta execução (idempotência)" 3

echo "=================================="
if [ "$falhas" -eq 0 ]; then
	echo "Fluxo de Matrícula completo, sem falhas."
	exit 0
else
	echo "$falhas etapa(s) com falha."
	exit 1
fi
