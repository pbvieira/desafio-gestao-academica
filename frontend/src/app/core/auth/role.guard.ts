import { CanActivateFn } from '@angular/router';
import { createAuthGuard } from 'keycloak-angular';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Autenticação já é garantida globalmente (initOptions.onLoad = 'login-required',
 * app.config.ts) - este guard só cobre RBAC por rota, via `route.data['papeis']`.
 * Sem `data.papeis`, a rota é liberada para qualquer usuário autenticado.
 */
export const roleGuard: CanActivateFn = createAuthGuard(async (route, _state, authData) => {
  const papeisPermitidos = route.data['papeis'] as string[] | undefined;
  if (!papeisPermitidos || papeisPermitidos.length === 0) {
    return authData.authenticated;
  }
  if (!authData.authenticated) {
    return false;
  }
  const temPapel = authData.grantedRoles.realmRoles.some((papel) => papeisPermitidos.includes(papel));
  if (temPapel) {
    return true;
  }
  const router = inject(Router);
  return router.parseUrl('/acesso-negado');
});
