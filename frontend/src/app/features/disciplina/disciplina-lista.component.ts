import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { DisciplinaService } from './disciplina.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';

@Component({
  selector: 'app-disciplina-lista',
  imports: [RouterLink],
  template: `
    <div class="toolbar">
      <h1>Disciplinas</h1>
      <a routerLink="/disciplinas/novo"><button class="primary">Nova disciplina</button></a>
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (disciplinaService.carregando()) {
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
          @for (disciplina of disciplinaService.disciplinas(); track disciplina.id) {
            <tr>
              <td>{{ disciplina.codigo }}</td>
              <td>{{ disciplina.nome }}</td>
              <td>{{ disciplina.ativo ? 'Ativa' : 'Inativa' }}</td>
              <td class="actions">
                <a [routerLink]="['/disciplinas', disciplina.id, 'editar']"><button>Editar</button></a>
                <button class="danger" (click)="excluir(disciplina.id, disciplina.nome)">Excluir</button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Nenhuma disciplina cadastrada.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class DisciplinaListaComponent implements OnInit {
  protected readonly disciplinaService = inject(DisciplinaService);
  protected readonly erro = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      await this.disciplinaService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  async excluir(id: number, nome: string): Promise<void> {
    if (!confirm(`Excluir a disciplina "${nome}"?`)) {
      return;
    }
    this.erro.set(null);
    try {
      await this.disciplinaService.excluir(id);
      await this.disciplinaService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }
}
