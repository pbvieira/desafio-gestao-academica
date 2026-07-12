import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CursoService } from './curso.service';
import { environment } from '../../../environments/environment';
import { CursoResponse } from '../../shared/models/curso.model';

describe('CursoService', () => {
  let service: CursoService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/cursos`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CursoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('carregar() faz GET e popula o signal cursos', async () => {
    const resposta: CursoResponse[] = [{ id: 1, codigo: 'C1', nome: 'Curso 1', ativo: true, criadoEm: '2026-01-01T00:00:00Z' }];

    const promessa = service.carregar();
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(resposta);
    await promessa;

    expect(service.cursos()).toEqual(resposta);
    expect(service.carregando()).toBeFalse();
  });

  it('carregar() zera carregando mesmo se a requisição falhar', async () => {
    const promessa = service.carregar();
    const req = httpMock.expectOne(baseUrl);
    req.flush('erro', { status: 500, statusText: 'Internal Server Error' });

    await expectAsync(promessa).toBeRejected();
    expect(service.carregando()).toBeFalse();
  });

  it('criar() faz POST com o corpo correto', async () => {
    const request = { codigo: 'C1', nome: 'Curso 1' };
    const resposta: CursoResponse = { id: 1, ...request, ativo: true, criadoEm: '2026-01-01T00:00:00Z' };

    const promessa = service.criar(request);
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(resposta);

    expect(await promessa).toEqual(resposta);
  });

  it('excluir() faz DELETE no id correto', async () => {
    const promessa = service.excluir(7);
    const req = httpMock.expectOne(`${baseUrl}/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    await promessa;
  });
});
