import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideKeycloak, includeBearerTokenInterceptor, INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG } from 'keycloak-angular';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { apiErrorInterceptor } from './core/error-handling/api-error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideKeycloak({
      config: environment.keycloak,
      initOptions: {
        onLoad: 'login-required',
        pkceMethod: 'S256',
      },
    }),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor, apiErrorInterceptor])),
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      // escapa caracteres especiais de regex na URL (ex: "." em "localhost:8080" não deve
      // casar com qualquer caractere) - achado de code review.
      useValue: [
        { urlPattern: new RegExp(`^${environment.apiBaseUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(/.*)?$`) },
      ],
    },
  ],
};
