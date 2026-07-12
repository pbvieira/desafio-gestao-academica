export interface DisciplinaResponse {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  criadoEm: string;
}

export interface DisciplinaRequest {
  codigo: string;
  nome: string;
}
