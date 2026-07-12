import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AlunoRequest, AlunoResponse } from '../../shared/models/aluno.model';

@Injectable({ providedIn: 'root' })
export class AlunoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/alunos`;

  readonly alunos = signal<AlunoResponse[]>([]);
  readonly carregando = signal(false);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const lista = await firstValueFrom(this.http.get<AlunoResponse[]>(this.baseUrl));
      this.alunos.set(lista);
    } finally {
      this.carregando.set(false);
    }
  }

  buscarPorId(id: number): Promise<AlunoResponse> {
    return firstValueFrom(this.http.get<AlunoResponse>(`${this.baseUrl}/${id}`));
  }

  /** D041/D040: autoleitura do próprio Aluno vinculado ao usuário logado (papel ALUNO). */
  meuPerfil(): Promise<AlunoResponse> {
    return firstValueFrom(this.http.get<AlunoResponse>(`${this.baseUrl}/me`));
  }

  criar(request: AlunoRequest): Promise<AlunoResponse> {
    return firstValueFrom(this.http.post<AlunoResponse>(this.baseUrl, request));
  }

  atualizar(id: number, request: AlunoRequest): Promise<AlunoResponse> {
    return firstValueFrom(this.http.put<AlunoResponse>(`${this.baseUrl}/${id}`, request));
  }

  async excluir(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.baseUrl}/${id}`));
  }
}
