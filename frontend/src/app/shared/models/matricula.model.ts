export type StatusMatricula = 'PENDENTE' | 'CONFIRMADA' | 'CANCELADA';

export interface MatriculaResponse {
  id: number;
  alunoId: number;
  alunoNome: string;
  turmaId: number;
  turmaCodigo: string;
  status: StatusMatricula;
  criadoEm: string;
  confirmadoEm: string | null;
  canceladoEm: string | null;
}

export interface MatriculaRequest {
  alunoId: number;
  turmaId: number;
}
