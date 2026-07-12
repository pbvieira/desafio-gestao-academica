import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DisciplinaRequest, DisciplinaResponse } from '../../shared/models/disciplina.model';

@Injectable({ providedIn: 'root' })
export class DisciplinaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/disciplinas`;

  readonly disciplinas = signal<DisciplinaResponse[]>([]);
  readonly carregando = signal(false);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const lista = await firstValueFrom(this.http.get<DisciplinaResponse[]>(this.baseUrl));
      this.disciplinas.set(lista);
    } finally {
      this.carregando.set(false);
    }
  }

  buscarPorId(id: number): Promise<DisciplinaResponse> {
    return firstValueFrom(this.http.get<DisciplinaResponse>(`${this.baseUrl}/${id}`));
  }

  criar(request: DisciplinaRequest): Promise<DisciplinaResponse> {
    return firstValueFrom(this.http.post<DisciplinaResponse>(this.baseUrl, request));
  }

  atualizar(id: number, request: DisciplinaRequest): Promise<DisciplinaResponse> {
    return firstValueFrom(this.http.put<DisciplinaResponse>(`${this.baseUrl}/${id}`, request));
  }

  async excluir(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.baseUrl}/${id}`));
  }
}
