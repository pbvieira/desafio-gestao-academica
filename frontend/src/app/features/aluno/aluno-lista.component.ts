import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AlunoService } from './aluno.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';

@Component({
  selector: 'app-aluno-lista',
  imports: [RouterLink],
  template: `
    <div class="toolbar">
      <h1>Alunos</h1>
      <a routerLink="/alunos/novo"><button class="primary">Novo aluno</button></a>
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (alunoService.carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Email</th>
            <th>Vinculado ao Keycloak</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (aluno of alunoService.alunos(); track aluno.id) {
            <tr>
              <td>{{ aluno.nome }}</td>
              <td>{{ aluno.email }}</td>
              <td>{{ aluno.keycloakSubjectId ? 'Sim' : 'Não' }}</td>
              <td>{{ aluno.ativo ? 'Ativo' : 'Inativo' }}</td>
              <td class="actions">
                <a [routerLink]="['/alunos', aluno.id, 'editar']"><button>Editar</button></a>
                <button class="danger" (click)="excluir(aluno.id, aluno.nome)">Excluir</button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="5" class="empty">Nenhum aluno cadastrado.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class AlunoListaComponent implements OnInit {
  protected readonly alunoService = inject(AlunoService);
  protected readonly erro = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      await this.alunoService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  async excluir(id: number, nome: string): Promise<void> {
    if (!confirm(`Excluir o aluno "${nome}"?`)) {
      return;
    }
    this.erro.set(null);
    try {
      await this.alunoService.excluir(id);
      await this.alunoService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }
}
