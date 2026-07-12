import { HttpErrorResponse } from '@angular/common/http';
import { problemDetailDe } from './problem-detail.util';

/** Mapa campo -> mensagem, para exibir erro de validação da API ao lado do input certo. */
export function errosPorCampo(erro: HttpErrorResponse): Record<string, string> {
  const problema = problemDetailDe(erro);
  if (erro.status !== 400 || !problema?.erros) {
    return {};
  }
  const mapa: Record<string, string> = {};
  for (const { campo, mensagem } of problema.erros) {
    mapa[campo] = mensagem;
  }
  return mapa;
}
