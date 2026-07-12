import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AlunoService } from './aluno.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';
import { AlunoResponse } from '../../shared/models/aluno.model';

/** D040/D041: consome GET /api/alunos/me (autoleitura, achado durante a Fase 5). */
@Component({
  selector: 'app-meu-perfil',
  imports: [DatePipe],
  template: `
    <h1>Meu perfil</h1>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (perfil(); as aluno) {
      <dl class="perfil">
        <dt>Nome</dt>
        <dd>{{ aluno.nome }}</dd>
        <dt>Email</dt>
        <dd>{{ aluno.email }}</dd>
        <dt>Cadastrado em</dt>
        <dd>{{ aluno.criadoEm | date: 'short' }}</dd>
      </dl>
    } @else if (!erro()) {
      <p class="loading">Carregando...</p>
    }
  `,
  styles: [
    `
      .perfil {
        display: grid;
        grid-template-columns: max-content 1fr;
        gap: var(--spacing-xs) var(--spacing-md);
      }
      .perfil dt {
        font-weight: 600;
        color: var(--color-muted);
      }
    `,
  ],
})
export class MeuPerfilComponent implements OnInit {
  private readonly alunoService = inject(AlunoService);

  protected readonly perfil = signal<AlunoResponse | null>(null);
  protected readonly erro = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      this.perfil.set(await this.alunoService.meuPerfil());
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }
}
