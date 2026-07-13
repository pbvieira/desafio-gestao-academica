export type Papel = 'ALUNO' | 'SECRETARIA' | 'ADMIN';

export interface UsuarioAdminResponse {
  id: string;
  username: string;
  nome: string;
  email: string;
  papel: Papel | null;
}

export interface ReatribuirPapelRequest {
  papel: Papel;
}
