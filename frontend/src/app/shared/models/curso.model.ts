export interface CursoResponse {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  criadoEm: string;
}

export interface CursoRequest {
  codigo: string;
  nome: string;
}
