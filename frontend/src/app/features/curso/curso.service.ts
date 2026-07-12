import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CursoRequest, CursoResponse } from '../../shared/models/curso.model';

@Injectable({ providedIn: 'root' })
export class CursoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/cursos`;

  readonly cursos = signal<CursoResponse[]>([]);
  readonly carregando = signal(false);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const lista = await firstValueFrom(this.http.get<CursoResponse[]>(this.baseUrl));
      this.cursos.set(lista);
    } finally {
      this.carregando.set(false);
    }
  }

  buscarPorId(id: number): Promise<CursoResponse> {
    return firstValueFrom(this.http.get<CursoResponse>(`${this.baseUrl}/${id}`));
  }

  criar(request: CursoRequest): Promise<CursoResponse> {
    return firstValueFrom(this.http.post<CursoResponse>(this.baseUrl, request));
  }

  atualizar(id: number, request: CursoRequest): Promise<CursoResponse> {
    return firstValueFrom(this.http.put<CursoResponse>(`${this.baseUrl}/${id}`, request));
  }

  async excluir(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.baseUrl}/${id}`));
  }
}
