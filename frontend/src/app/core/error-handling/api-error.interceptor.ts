import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { catchError, throwError } from 'rxjs';
import { ErrorBannerService } from './error-banner.service';

/**
 * Trata de forma centralizada só o que nenhuma tela deveria ter que pensar a cada
 * chamada: 401 (sessão expirada/inválida - reautentica) e falha de rede (API fora do
 * ar). 403/404/409/400 continuam propagando para quem chamou, que decide a UX certa
 * (ex: MatriculaService trata 409 de vaga perdida com mensagem específica; formulários
 * tratam 400 por campo via ProblemDetail.erros) - ver core/error-handling/problem-detail.util.ts
 * para o fallback genérico usado por quem não tem tratamento mais específico.
 */
export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(Keycloak);
  const errorBanner = inject(ErrorBannerService);

  return next(req).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse) {
        if (erro.status === 401) {
          keycloak.login();
        } else if (erro.status === 0) {
          errorBanner.show('Não foi possível conectar à API. Verifique sua conexão e tente novamente.');
        } else if (erro.status === 403) {
          errorBanner.show('Você não tem permissão para realizar esta ação.');
        }
      }
      return throwError(() => erro);
    }),
  );
};
