import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TurmaService } from './turma.service';
import { MatriculaService } from '../matricula/matricula.service';
import { AlunoService } from '../aluno/aluno.service';
import { CurrentUser } from '../../core/auth/current-user';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';
import { TurmaResponse } from '../../shared/models/turma.model';

@Component({
  selector: 'app-turma-lista',
  imports: [RouterLink],
  template: `
    <div class="toolbar">
      <h1>Turmas</h1>
      @if (currentUser.ehSecretariaOuAdmin) {
        <a routerLink="/turmas/novo"><button class="primary">Nova turma</button></a>
      }
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }
    @if (sucesso(); as mensagem) {
      <div class="banner banner-success">{{ mensagem }}</div>
    }

    @if (turmaService.carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Código</th>
            <th>Curso</th>
            <th>Disciplina</th>
            <th>Vagas</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          @for (turma of turmaService.turmas(); track turma.id) {
            <tr>
              <td>{{ turma.codigo }}</td>
              <td>{{ turma.cursoNome }}</td>
              <td>{{ turma.disciplinaNome }}</td>
              <td>{{ turma.vagasOcupadas }} / {{ turma.limiteVagas }}</td>
              <td>
                <span class="badge" [class]="'badge-' + turma.status.toLowerCase()">{{ turma.status }}</span>
              </td>
              <td class="actions">
                @if (currentUser.temPapel('ALUNO')) {
                  <button
                    class="primary"
                    [disabled]="!podeMatricular(turma) || matriculando() === turma.id"
                    (click)="matricularSe(turma)"
                  >
                    Matricular-se
                  </button>
                }
                @if (currentUser.ehSecretariaOuAdmin) {
                  <a [routerLink]="['/turmas', turma.id, 'matriculas']"><button>Matrículas</button></a>
                  <a [routerLink]="['/turmas', turma.id, 'editar']"><button>Editar</button></a>
                  <button class="danger" (click)="excluir(turma.id, turma.codigo)">Excluir</button>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6" class="empty">Nenhuma turma cadastrada.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class TurmaListaComponent implements OnInit {
  protected readonly turmaService = inject(TurmaService);
  protected readonly currentUser = inject(CurrentUser);
  private readonly matriculaService = inject(MatriculaService);
  private readonly alunoService = inject(AlunoService);

  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);
  protected readonly matriculando = signal<number | null>(null);

  private meuAlunoId: number | null = null;

  async ngOnInit(): Promise<void> {
    try {
      await this.turmaService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  podeMatricular(turma: TurmaResponse): boolean {
    return turma.status === 'ABERTA' && turma.vagasOcupadas < turma.limiteVagas;
  }

  async matricularSe(turma: TurmaResponse): Promise<void> {
    this.erro.set(null);
    this.sucesso.set(null);
    this.matriculando.set(turma.id);
    try {
      if (this.meuAlunoId == null) {
        const meuPerfil = await this.alunoService.meuPerfil();
        this.meuAlunoId = meuPerfil.id;
      }
      await this.matriculaService.criar({ alunoId: this.meuAlunoId, turmaId: turma.id });
      this.sucesso.set(`Matrícula criada na turma "${turma.codigo}" (aguardando confirmação da secretaria).`);
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    } finally {
      this.matriculando.set(null);
    }
  }

  async excluir(id: number, codigo: string): Promise<void> {
    if (!confirm(`Excluir a turma "${codigo}"?`)) {
      return;
    }
    this.erro.set(null);
    try {
      await this.turmaService.excluir(id);
      await this.turmaService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }
}
