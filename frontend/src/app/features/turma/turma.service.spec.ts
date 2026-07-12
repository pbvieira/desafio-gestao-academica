import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TurmaService } from './turma.service';
import { environment } from '../../../environments/environment';
import { TurmaResponse } from '../../shared/models/turma.model';

describe('TurmaService', () => {
  let service: TurmaService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/turmas`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TurmaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  const turmaExemplo: TurmaResponse = {
    id: 1,
    codigo: 'T1',
    cursoId: 1,
    cursoNome: 'Curso 1',
    disciplinaId: 1,
    disciplinaNome: 'Disciplina 1',
    limiteVagas: 30,
    vagasOcupadas: 5,
    status: 'ABERTA',
    ativo: true,
    criadoEm: '2026-01-01T00:00:00Z',
  };

  it('carregar() faz GET e popula o signal turmas', async () => {
    const promessa = service.carregar();
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([turmaExemplo]);
    await promessa;

    expect(service.turmas()).toEqual([turmaExemplo]);
  });

  it('criar() faz POST com cursoId/disciplinaId/limiteVagas', async () => {
    const request = { codigo: 'T1', cursoId: 1, disciplinaId: 1, limiteVagas: 30 };

    const promessa = service.criar(request);
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(turmaExemplo);

    expect(await promessa).toEqual(turmaExemplo);
  });

  it('buscarPorId() faz GET no id correto', async () => {
    const promessa = service.buscarPorId(1);
    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(turmaExemplo);

    expect(await promessa).toEqual(turmaExemplo);
  });
});
