import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AlunoService } from './aluno.service';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';
import { errosPorCampo } from '../../core/error-handling/erros-por-campo.util';

@Component({
  selector: 'app-aluno-form',
  imports: [ReactiveFormsModule],
  template: `
    <h1>{{ id() ? 'Editar aluno' : 'Novo aluno' }}</h1>

    @if (erroGeral(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    <form [formGroup]="form" (ngSubmit)="salvar()">
      <div class="field">
        <label for="nome">Nome</label>
        <input id="nome" formControlName="nome" maxlength="200" />
        @if (form.controls.nome.touched && form.controls.nome.invalid) {
          <div class="field-error">Nome é obrigatório (até 200 caracteres).</div>
        }
        @if (errosApi()['nome']; as msg) {
          <div class="field-error">{{ msg }}</div>
        }
      </div>

      <div class="field">
        <label for="email">Email</label>
        <input id="email" type="email" formControlName="email" maxlength="200" />
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <div class="field-error">Email válido é obrigatório (até 200 caracteres).</div>
        }
        @if (errosApi()['email']; as msg) {
          <div class="field-error">{{ msg }}</div>
        }
      </div>

      <div class="field">
        <label for="keycloakSubjectId">ID do usuário no Keycloak (opcional)</label>
        <input id="keycloakSubjectId" formControlName="keycloakSubjectId" maxlength="64" />
        @if (errosApi()['keycloakSubjectId']; as msg) {
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
export class AlunoFormComponent implements OnInit {
  private readonly alunoService = inject(AlunoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly id = signal<number | null>(null);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly errosApi = signal<Record<string, string>>({});
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    keycloakSubjectId: ['', [Validators.maxLength(64)]],
  });

  async ngOnInit(): Promise<void> {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }
    const id = Number(idParam);
    this.id.set(id);
    try {
      const aluno = await this.alunoService.buscarPorId(id);
      this.form.setValue({
        nome: aluno.nome,
        email: aluno.email,
        keycloakSubjectId: aluno.keycloakSubjectId ?? '',
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
        nome: valor.nome,
        email: valor.email,
        keycloakSubjectId: valor.keycloakSubjectId || null,
      };
      if (this.id() != null) {
        await this.alunoService.atualizar(this.id()!, request);
      } else {
        await this.alunoService.criar(request);
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
    this.router.navigate(['/alunos']);
  }
}
