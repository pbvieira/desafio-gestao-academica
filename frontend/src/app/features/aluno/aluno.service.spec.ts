import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AlunoService } from './aluno.service';
import { environment } from '../../../environments/environment';
import { AlunoResponse } from '../../shared/models/aluno.model';

describe('AlunoService', () => {
  let service: AlunoService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/alunos`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AlunoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('meuPerfil() faz GET em /api/alunos/me (D040/D041)', async () => {
    const resposta: AlunoResponse = {
      id: 1,
      nome: 'Ana Silva',
      email: 'ana@example.com',
      keycloakSubjectId: 'sub-123',
      ativo: true,
      criadoEm: '2026-01-01T00:00:00Z',
    };

    const promessa = service.meuPerfil();
    const req = httpMock.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush(resposta);

    expect(await promessa).toEqual(resposta);
  });

  it('criar() envia keycloakSubjectId nulo quando não vinculado', async () => {
    const request = { nome: 'Ana Silva', email: 'ana@example.com', keycloakSubjectId: null };

    const promessa = service.criar(request);
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.body.keycloakSubjectId).toBeNull();
    req.flush({ id: 1, ...request, ativo: true, criadoEm: '2026-01-01T00:00:00Z' });

    await promessa;
  });
});
