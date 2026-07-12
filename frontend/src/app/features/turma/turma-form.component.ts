import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TurmaService } from './turma.service';
import { CursoService } from '../curso/curso.service';
import { DisciplinaService } from '../disciplina/disciplina.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';
import { errosPorCampo } from '../../core/error-handling/erros-por-campo.util';

@Component({
  selector: 'app-turma-form',
  imports: [ReactiveFormsModule],
  template: `
    <h1>{{ id() ? 'Editar turma' : 'Nova turma' }}</h1>

    @if (erroGeral(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    <form [formGroup]="form" (ngSubmit)="salvar()">
      <div class="field">
        <label for="codigo">Código</label>
        <input id="codigo" formControlName="codigo" maxlength="20" />
        @if (form.controls.codigo.touched && form.controls.codigo.invalid) {
          <div class="field-error">Código é obrigatório (até 20 caracteres).</div>
        }
        @if (errosApi()['codigo']; as msg) {
          <div class="field-error">{{ msg }}</div>
        }
      </div>

      <div class="field">
        <label for="cursoId">Curso</label>
        <select id="cursoId" formControlName="cursoId">
          <option [ngValue]="null">Selecione...</option>
          @for (curso of cursoService.cursos(); track curso.id) {
            <option [ngValue]="curso.id">{{ curso.nome }}</option>
          }
        </select>
        @if (form.controls.cursoId.touched && form.controls.cursoId.invalid) {
          <div class="field-error">Curso é obrigatório.</div>
        }
        @if (errosApi()['cursoId']; as msg) {
          <div class="field-error">{{ msg }}</div>
        }
      </div>

      <div class="field">
        <label for="disciplinaId">Disciplina</label>
        <select id="disciplinaId" formControlName="disciplinaId">
          <option [ngValue]="null">Selecione...</option>
          @for (disciplina of disciplinaService.disciplinas(); track disciplina.id) {
            <option [ngValue]="disciplina.id">{{ disciplina.nome }}</option>
          }
        </select>
        @if (form.controls.disciplinaId.touched && form.controls.disciplinaId.invalid) {
          <div class="field-error">Disciplina é obrigatória.</div>
        }
        @if (errosApi()['disciplinaId']; as msg) {
          <div class="field-error">{{ msg }}</div>
        }
      </div>

      <div class="field">
        <label for="limiteVagas">Limite de vagas</label>
        <input id="limiteVagas" type="number" formControlName="limiteVagas" min="1" />
        @if (form.controls.limiteVagas.touched && form.controls.limiteVagas.invalid) {
          <div class="field-error">Limite de vagas deve ser maior que zero.</div>
        }
        @if (errosApi()['limiteVagas']; as msg) {
          <div class="field-error">{{ msg }}</div>
        }
      </div>

      <div class="toolbar">
        <button type="button" (click)="voltar()">Cancelar</button>
        <button type="submit" class="primary" [disabled]="form.invalid || salvando()">Salvar</button>
      </div>
    </form>
  `,
})
export class TurmaFormComponent implements OnInit {
  private readonly turmaService = inject(TurmaService);
  protected readonly cursoService = inject(CursoService);
  protected readonly disciplinaService = inject(DisciplinaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly id = signal<number | null>(null);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly errosApi = signal<Record<string, string>>({});
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    cursoId: this.fb.control<number | null>(null, Validators.required),
    disciplinaId: this.fb.control<number | null>(null, Validators.required),
    limiteVagas: [1, [Validators.required, Validators.min(1)]],
  });

  async ngOnInit(): Promise<void> {
    try {
      await Promise.all([this.cursoService.carregar(), this.disciplinaService.carregar()]);

      const idParam = this.route.snapshot.paramMap.get('id');
      if (!idParam) {
        return;
      }
      const id = Number(idParam);
      this.id.set(id);
      const turma = await this.turmaService.buscarPorId(id);
      this.form.setValue({
        codigo: turma.codigo,
        cursoId: turma.cursoId,
        disciplinaId: turma.disciplinaId,
        limiteVagas: turma.limiteVagas,
      });
    } catch (e) {
      this.erroGeral.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  async salvar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.erroGeral.set(null);
    this.errosApi.set({});
    this.salvando.set(true);
    try {
      const valor = this.form.getRawValue();
      const request = {
        codigo: valor.codigo,
        cursoId: valor.cursoId!,
        disciplinaId: valor.disciplinaId!,
        limiteVagas: valor.limiteVagas,
      };
      if (this.id() != null) {
        await this.turmaService.atualizar(this.id()!, request);
      } else {
        await this.turmaService.criar(request);
      }
      this.voltar();
    } catch (e) {
      const erro = e as HttpErrorResponse;
      const campos = errosPorCampo(erro);
      if (Object.keys(campos).length > 0) {
        this.errosApi.set(campos);
      } else {
        this.erroGeral.set(mensagemAmigavel(erro));
      }
    } finally {
      this.salvando.set(false);
    }
  }

  voltar(): void {
    this.router.navigate(['/turmas']);
  }
}
