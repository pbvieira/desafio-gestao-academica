import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuarioAdminService } from './usuario-admin.service';
import { Papel } from '../../shared/models/usuario-admin.model';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';

const PAPEIS: Papel[] = ['ALUNO', 'SECRETARIA', 'ADMIN'];

@Component({
  selector: 'app-usuarios-lista',
  template: `
    <div class="toolbar">
      <h1>Usuários e Papéis</h1>
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (usuarioAdminService.carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Usuário</th>
            <th>Email</th>
            <th>Papel</th>
          </tr>
        </thead>
        <tbody>
          @for (usuario of usuarioAdminService.usuarios(); track usuario.id) {
            <tr>
              <td>{{ usuario.nome }}</td>
              <td>{{ usuario.username }}</td>
              <td>{{ usuario.email }}</td>
              <td>
                <select
                  [value]="usuario.papel"
                  (change)="onPapelAlterado(usuario.id, $event)"
                  [disabled]="alterando() === usuario.id"
                >
                  @for (papel of papeis; track papel) {
                    <option [value]="papel">{{ papel }}</option>
                  }
                </select>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Nenhum usuário encontrado.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class UsuariosListaComponent implements OnInit {
  protected readonly usuarioAdminService = inject(UsuarioAdminService);
  protected readonly erro = signal<string | null>(null);
  protected readonly alterando = signal<string | null>(null);
  protected readonly papeis = PAPEIS;

  async ngOnInit(): Promise<void> {
    try {
      await this.usuarioAdminService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  async onPapelAlterado(id: string, evento: Event): Promise<void> {
    const novoPapel = (evento.target as HTMLSelectElement).value as Papel;
    this.erro.set(null);
    this.alterando.set(id);
    try {
      await this.usuarioAdminService.reatribuirPapel(id, novoPapel);
      await this.usuarioAdminService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    } finally {
      this.alterando.set(null);
    }
  }
}
