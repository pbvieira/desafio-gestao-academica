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
