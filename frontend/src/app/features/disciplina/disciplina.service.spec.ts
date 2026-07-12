import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DisciplinaService } from './disciplina.service';
import { environment } from '../../../environments/environment';
import { DisciplinaResponse } from '../../shared/models/disciplina.model';

describe('DisciplinaService', () => {
  let service: DisciplinaService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/disciplinas`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DisciplinaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('carregar() faz GET e popula o signal disciplinas', async () => {
    const resposta: DisciplinaResponse[] = [
      { id: 1, codigo: 'D1', nome: 'Disciplina 1', ativo: true, criadoEm: '2026-01-01T00:00:00Z' },
    ];

    const promessa = service.carregar();
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(resposta);
    await promessa;

    expect(service.disciplinas()).toEqual(resposta);
  });

  it('atualizar() faz PUT no id correto com o corpo certo', async () => {
    const request = { codigo: 'D1', nome: 'Disciplina Atualizada' };
    const resposta: DisciplinaResponse = { id: 3, ...request, ativo: true, criadoEm: '2026-01-01T00:00:00Z' };

    const promessa = service.atualizar(3, request);
    const req = httpMock.expectOne(`${baseUrl}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(resposta);

    expect(await promessa).toEqual(resposta);
  });

  it('excluir() faz DELETE no id correto', async () => {
    const promessa = service.excluir(9);
    const req = httpMock.expectOne(`${baseUrl}/9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    await promessa;
  });
});
