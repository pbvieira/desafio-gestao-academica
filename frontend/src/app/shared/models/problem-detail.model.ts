export interface ErroValidacaoCampo {
  campo: string;
  mensagem: string;
}

/** Espelha br.com.desafio.tecnico.gestao.errorhandling.ProblemDetailFactory (RFC 7807 + extensões). */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string | null;
  errorCode: string;
  timestamp: string;
  path: string;
  erros?: ErroValidacaoCampo[];
}
