import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatriculaService } from './matricula.service';
import { environment } from '../../../environments/environment';
import { MatriculaResponse } from '../../shared/models/matricula.model';

describe('MatriculaService', () => {
  let service: MatriculaService;
  let httpMock: HttpTestingController;

  const matriculaExemplo: MatriculaResponse = {
    id: 1,
    alunoId: 1,
    alunoNome: 'Ana Silva',
    turmaId: 1,
    turmaCodigo: 'T1',
    status: 'PENDENTE',
    criadoEm: '2026-01-01T00:00:00Z',
    confirmadoEm: null,
    canceladoEm: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MatriculaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('criar() faz POST em /api/matriculas com alunoId/turmaId', async () => {
    const promessa = service.criar({ alunoId: 1, turmaId: 1 });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/matriculas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ alunoId: 1, turmaId: 1 });
    req.flush(matriculaExemplo);

    await promessa;
  });

  it('confirmar() faz POST em /api/matriculas/{id}/confirmar', async () => {
    const promessa = service.confirmar(1);
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/matriculas/1/confirmar`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...matriculaExemplo, status: 'CONFIRMADA' });

    await promessa;
  });

  it('cancelar() faz POST em /api/matriculas/{id}/cancelar', async () => {
    const promessa = service.cancelar(1);
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/matriculas/1/cancelar`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...matriculaExemplo, status: 'CANCELADA' });

    await promessa;
  });

  it('listarPorAluno() faz GET em /api/alunos/{alunoId}/matriculas', async () => {
    const promessa = service.listarPorAluno(1);
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/alunos/1/matriculas`);
    expect(req.request.method).toBe('GET');
    req.flush([matriculaExemplo]);

    expect(await promessa).toEqual([matriculaExemplo]);
  });

  it('listarPorTurma() faz GET em /api/turmas/{turmaId}/matriculas', async () => {
    const promessa = service.listarPorTurma(1);
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/turmas/1/matriculas`);
    expect(req.request.method).toBe('GET');
    req.flush([matriculaExemplo]);

    expect(await promessa).toEqual([matriculaExemplo]);
  });

  it('confirmar() rejeita a promise em 409 (para o chamador tratar vaga perdida)', async () => {
    const promessa = service.confirmar(1);
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/matriculas/1/confirmar`);
    req.flush(
      { errorCode: 'VAGAS_ESGOTADAS', status: 409, detail: 'Sem vagas.' },
      { status: 409, statusText: 'Conflict' },
    );

    await expectAsync(promessa).toBeRejected();
  });
});
