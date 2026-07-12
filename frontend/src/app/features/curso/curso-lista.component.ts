import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CursoService } from './curso.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';

@Component({
  selector: 'app-curso-lista',
  imports: [RouterLink],
  template: `
    <div class="toolbar">
      <h1>Cursos</h1>
      <a routerLink="/cursos/novo"><button class="primary">Novo curso</button></a>
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (cursoService.carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Código</th>
            <th>Nome</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (curso of cursoService.cursos(); track curso.id) {
            <tr>
              <td>{{ curso.codigo }}</td>
              <td>{{ curso.nome }}</td>
              <td>{{ curso.ativo ? 'Ativo' : 'Inativo' }}</td>
              <td class="actions">
                <a [routerLink]="['/cursos', curso.id, 'editar']"><button>Editar</button></a>
                <button class="danger" (click)="excluir(curso.id, curso.nome)">Excluir</button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Nenhum curso cadastrado.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class CursoListaComponent implements OnInit {
  protected readonly cursoService = inject(CursoService);
  protected readonly erro = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      await this.cursoService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  async excluir(id: number, nome: string): Promise<void> {
    if (!confirm(`Excluir o curso "${nome}"?`)) {
      return;
    }
    this.erro.set(null);
    try {
      await this.cursoService.excluir(id);
      await this.cursoService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }
}
