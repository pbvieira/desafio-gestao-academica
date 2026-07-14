import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { UsuariosListaComponent } from './usuarios-lista.component';
import { UsuarioAdminService } from './usuario-admin.service';

describe('UsuariosListaComponent', () => {
  let fixture: ComponentFixture<UsuariosListaComponent>;

  beforeEach(async () => {
    const serviceStub = {
      usuarios: signal([
        { id: '1', username: 'aluno.teste', nome: 'Aluno Teste', email: 'a@a.com', papel: 'ALUNO' },
        { id: '2', username: 'admin.teste', nome: 'Admin Teste', email: 'ad@a.com', papel: 'ADMIN' },
        { id: '3', username: 'secretaria.teste', nome: 'Secretaria Teste', email: 's@a.com', papel: 'SECRETARIA' },
      ]),
      carregando: signal(false),
      carregar: async () => {},
    };

    await TestBed.configureTestingModule({
      imports: [UsuariosListaComponent],
      providers: [{ provide: UsuarioAdminService, useValue: serviceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(UsuariosListaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('exibe no seletor de papel o papel atual de cada usuário, não sempre o primeiro da lista', () => {
    const selects: HTMLSelectElement[] = Array.from(fixture.nativeElement.querySelectorAll('select'));

    expect(selects.length).toBe(3);
    expect(selects[0].value).toBe('ALUNO');
    expect(selects[1].value).toBe('ADMIN');
    expect(selects[2].value).toBe('SECRETARIA');
  });
});
