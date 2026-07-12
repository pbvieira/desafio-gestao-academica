import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatriculaService } from './matricula.service';
import { TurmaService } from '../turma/turma.service';
import { mensagemAmigavel, problemDetailDe } from '../../core/error-handling/problem-detail.util';
import { MatriculaResponse } from '../../shared/models/matricula.model';

const CODIGOS_VAGA_PERDIDA = new Set(['VAGAS_ESGOTADAS', 'CONFLITO_CONCORRENCIA']);

/**
 * specs/008-frontend-angular.md, seção 4.5: o momento de UX mais citado como critério
 * eliminatório do PRD - confirmar é onde a vaga é de fato consumida (specs/006,
 * D024/D025), então é aqui, não na criação da matrícula, que duas requisições podem
 * disputar a última vaga de verdade.
 */
@Component({
  selector: 'app-matriculas-por-turma',
  imports: [DatePipe, RouterLink],
  template: `
    <div class="toolbar">
      <h1>Matrículas da turma {{ turmaCodigo() }}</h1>
      <a routerLink="/turmas"><button>Voltar</button></a>
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }
    @if (vagaPerdida()) {
      <div class="banner banner-error">
        Esta vaga foi confirmada por outra requisição nos últimos instantes — a turma pode não ter mais vagas
        disponíveis. Atualize a lista e verifique a situação da turma antes de tentar novamente.
      </div>
    }

    @if (carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Aluno</th>
            <th>Status</th>
            <th>Criada em</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (matricula of matriculas(); track matricula.id) {
            <tr>
              <td>{{ matricula.alunoNome }}</td>
              <td>
                <span class="badge" [class]="'badge-' + matricula.status.toLowerCase()">{{ matricula.status }}</span>
              </td>
              <td>{{ matricula.criadoEm | date: 'short' }}</td>
              <td class="actions">
                @if (matricula.status === 'PENDENTE') {
                  <button class="primary" [disabled]="confirmando() === matricula.id" (click)="confirmar(matricula)">
                    Confirmar
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Nenhuma matrícula nesta turma.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class MatriculasPorTurmaComponent implements OnInit {
  private readonly matriculaService = inject(MatriculaService);
  private readonly turmaService = inject(TurmaService);
  private readonly route = inject(ActivatedRoute);

  protected readonly matriculas = signal<MatriculaResponse[]>([]);
  protected readonly turmaCodigo = signal('');
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly vagaPerdida = signal(false);
  protected readonly confirmando = signal<number | null>(null);

  private turmaId!: number;

  async ngOnInit(): Promise<void> {
    this.turmaId = Number(this.route.snapshot.paramMap.get('id'));
    try {
      const turma = await this.turmaService.buscarPorId(this.turmaId);
      this.turmaCodigo.set(turma.codigo);
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
      return;
    }
    await this.carregar();
  }

  private async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set(null);
    try {
      this.matriculas.set(await this.matriculaService.listarPorTurma(this.turmaId));
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    } finally {
      this.carregando.set(false);
    }
  }

  async confirmar(matricula: MatriculaResponse): Promise<void> {
    this.erro.set(null);
    this.vagaPerdida.set(false);
    this.confirmando.set(matricula.id);
    try {
      await this.matriculaService.confirmar(matricula.id);
      await this.carregar();
    } catch (e) {
      const erro = e as HttpErrorResponse;
      const problema = problemDetailDe(erro);
      if (erro.status === 409 && problema && CODIGOS_VAGA_PERDIDA.has(problema.errorCode)) {
        this.vagaPerdida.set(true);
      } else {
        this.erro.set(mensagemAmigavel(erro));
      }
    } finally {
      this.confirmando.set(null);
    }
  }
}
