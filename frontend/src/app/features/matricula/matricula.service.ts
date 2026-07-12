import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MatriculaRequest, MatriculaResponse } from '../../shared/models/matricula.model';

/**
 * A regra de vagas (specs/006, D024/D025) é aplicada no CONFIRMAR, não na criação da
 * matrícula (que só valida turma aberta + duplicidade) - é aqui, não em `criar()`, que
 * a corrida pela última vaga acontece de verdade (PRD, critério eliminatório mais
 * citado). O tratamento de UX específico do 409 (seção 4.5 da spec 008) fica no
 * componente que chama `confirmar()`.
 */
@Injectable({ providedIn: 'root' })
export class MatriculaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/matriculas`;

  criar(request: MatriculaRequest): Promise<MatriculaResponse> {
    return firstValueFrom(this.http.post<MatriculaResponse>(this.baseUrl, request));
  }

  confirmar(id: number): Promise<MatriculaResponse> {
    return firstValueFrom(this.http.post<MatriculaResponse>(`${this.baseUrl}/${id}/confirmar`, {}));
  }

  cancelar(id: number): Promise<MatriculaResponse> {
    return firstValueFrom(this.http.post<MatriculaResponse>(`${this.baseUrl}/${id}/cancelar`, {}));
  }

  listarPorAluno(alunoId: number): Promise<MatriculaResponse[]> {
    return firstValueFrom(
      this.http.get<MatriculaResponse[]>(`${environment.apiBaseUrl}/api/alunos/${alunoId}/matriculas`),
    );
  }

  listarPorTurma(turmaId: number): Promise<MatriculaResponse[]> {
    return firstValueFrom(
      this.http.get<MatriculaResponse[]>(`${environment.apiBaseUrl}/api/turmas/${turmaId}/matriculas`),
    );
  }
}
