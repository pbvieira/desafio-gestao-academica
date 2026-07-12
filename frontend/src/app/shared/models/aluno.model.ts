export interface AlunoResponse {
  id: number;
  nome: string;
  email: string;
  keycloakSubjectId: string | null;
  ativo: boolean;
  criadoEm: string;
}

export interface AlunoRequest {
  nome: string;
  email: string;
  keycloakSubjectId?: string | null;
}
