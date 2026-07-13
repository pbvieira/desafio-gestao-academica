import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Papel, UsuarioAdminResponse } from '../../shared/models/usuario-admin.model';

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/usuarios`;

  readonly usuarios = signal<UsuarioAdminResponse[]>([]);
  readonly carregando = signal(false);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const lista = await firstValueFrom(this.http.get<UsuarioAdminResponse[]>(this.baseUrl));
      this.usuarios.set(lista);
    } finally {
      this.carregando.set(false);
    }
  }

  async reatribuirPapel(id: string, papel: Papel): Promise<void> {
    await firstValueFrom(this.http.patch<void>(`${this.baseUrl}/${id}/papel`, { papel }));
  }
}
