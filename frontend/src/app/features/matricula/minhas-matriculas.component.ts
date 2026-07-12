import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatriculaService } from './matricula.service';
import { AlunoService } from '../aluno/aluno.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';
import { MatriculaResponse } from '../../shared/models/matricula.model';

@Component({
  selector: 'app-minhas-matriculas',
  imports: [DatePipe],
  template: `
    <h1>Minhas matrículas</h1>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Turma</th>
            <th>Status</th>
            <th>Criada em</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (matricula of matriculas(); track matricula.id) {
            <tr>
              <td>{{ matricula.turmaCodigo }}</td>
              <td>
                <span class="badge" [class]="'badge-' + matricula.status.toLowerCase()">{{ matricula.status }}</span>
              </td>
              <td>{{ matricula.criadoEm | date: 'short' }}</td>
              <td class="actions">
                @if (matricula.status !== 'CANCELADA') {
                  <button class="danger" [disabled]="cancelando() === matricula.id" (click)="cancelar(matricula)">
                    Cancelar
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Você ainda não tem matrículas.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class MinhasMatriculasComponent implements OnInit {
  private readonly matriculaService = inject(MatriculaService);
  private readonly alunoService = inject(AlunoService);

  protected readonly matriculas = signal<MatriculaResponse[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly cancelando = signal<number | null>(null);

  private meuAlunoId: number | null = null;

  async ngOnInit(): Promise<void> {
    await this.carregar();
  }

  private async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set(null);
    try {
      if (this.meuAlunoId == null) {
        const meuPerfil = await this.alunoService.meuPerfil();
        this.meuAlunoId = meuPerfil.id;
      }
      this.matriculas.set(await this.matriculaService.listarPorAluno(this.meuAlunoId));
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    } finally {
      this.carregando.set(false);
    }
  }

  async cancelar(matricula: MatriculaResponse): Promise<void> {
    if (!confirm(`Cancelar a matrícula na turma "${matricula.turmaCodigo}"?`)) {
      return;
    }
    this.erro.set(null);
    this.cancelando.set(matricula.id);
    try {
      await this.matriculaService.cancelar(matricula.id);
      await this.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    } finally {
      this.cancelando.set(null);
    }
  }
}
