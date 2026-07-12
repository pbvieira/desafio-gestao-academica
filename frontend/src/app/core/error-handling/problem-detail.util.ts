import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../../shared/models/problem-detail.model';

/** Extrai o ProblemDetail do corpo do erro, se presente e no formato esperado. */
export function problemDetailDe(erro: HttpErrorResponse): ProblemDetail | null {
  const corpo = erro.error;
  if (corpo && typeof corpo === 'object' && typeof corpo.status === 'number') {
    return corpo as ProblemDetail;
  }
  return null;
}

/**
 * Mensagem de fallback genérica por status - usada por quem não tem um tratamento mais
 * específico (ex: 409 de vaga perdida em Matrícula tem sua própria mensagem, não usa isto).
 */
export function mensagemAmigavel(erro: HttpErrorResponse): string {
  const problema = problemDetailDe(erro);
  if (erro.status === 0) {
    return 'Não foi possível conectar à API. Verifique sua conexão e tente novamente.';
  }
  switch (erro.status) {
    case 404:
      return problema?.detail || 'Recurso não encontrado.';
    case 409:
      return problema?.detail || 'Conflito ao processar a solicitação. Tente novamente.';
    case 400:
      return problema?.detail || 'Dados inválidos. Verifique os campos e tente novamente.';
    case 500:
      return 'Erro interno no servidor. Tente novamente em instantes.';
    default:
      return problema?.detail || 'Ocorreu um erro inesperado.';
  }
}
