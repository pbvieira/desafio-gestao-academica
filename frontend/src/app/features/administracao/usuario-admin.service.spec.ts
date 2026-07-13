import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UsuarioAdminService } from './usuario-admin.service';
import { environment } from '../../../environments/environment';
import { UsuarioAdminResponse } from '../../shared/models/usuario-admin.model';

describe('UsuarioAdminService', () => {
  let service: UsuarioAdminService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/admin/usuarios`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UsuarioAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('carrega a lista de usuarios e atualiza o signal', async () => {
    const usuarios: UsuarioAdminResponse[] = [
      { id: 'kc-1', username: 'aluno.teste', nome: 'Aluno Teste', email: 'a@a.com', papel: 'ALUNO' },
    ];
    const promise = service.carregar();
    httpMock.expectOne(baseUrl).flush(usuarios);
    await promise;
    expect(service.usuarios()).toEqual(usuarios);
  });

  it('reatribui papel via PATCH', async () => {
    const promise = service.reatribuirPapel('kc-1', 'ADMIN');
    const req = httpMock.expectOne(`${baseUrl}/kc-1/papel`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ papel: 'ADMIN' });
    req.flush(null, { status: 204, statusText: 'No Content' });
    await promise;
  });
});
