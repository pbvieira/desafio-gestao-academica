import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TurmaRequest, TurmaResponse } from '../../shared/models/turma.model';

@Injectable({ providedIn: 'root' })
export class TurmaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/turmas`;

  readonly turmas = signal<TurmaResponse[]>([]);
  readonly carregando = signal(false);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const lista = await firstValueFrom(this.http.get<TurmaResponse[]>(this.baseUrl));
      this.turmas.set(lista);
    } finally {
      this.carregando.set(false);
    }
  }

  buscarPorId(id: number): Promise<TurmaResponse> {
    return firstValueFrom(this.http.get<TurmaResponse>(`${this.baseUrl}/${id}`));
  }

  criar(request: TurmaRequest): Promise<TurmaResponse> {
    return firstValueFrom(this.http.post<TurmaResponse>(this.baseUrl, request));
  }

  atualizar(id: number, request: TurmaRequest): Promise<TurmaResponse> {
    return firstValueFrom(this.http.put<TurmaResponse>(`${this.baseUrl}/${id}`, request));
  }

  async excluir(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.baseUrl}/${id}`));
  }
}
