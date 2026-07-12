export type StatusTurma = 'ABERTA' | 'FECHADA';

export interface TurmaResponse {
  id: number;
  codigo: string;
  cursoId: number;
  cursoNome: string;
  disciplinaId: number;
  disciplinaNome: string;
  limiteVagas: number;
  vagasOcupadas: number;
  status: StatusTurma;
  ativo: boolean;
  criadoEm: string;
}

export interface TurmaRequest {
  codigo: string;
  cursoId: number;
  disciplinaId: number;
  limiteVagas: number;
}
