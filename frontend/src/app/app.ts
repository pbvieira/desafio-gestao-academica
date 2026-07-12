import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';
import { CurrentUser } from './core/auth/current-user';
import { ErrorBannerService } from './core/error-handling/error-banner.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly currentUser = inject(CurrentUser);
  protected readonly errorBanner = inject(ErrorBannerService);
  private readonly keycloak = inject(Keycloak);

  protected get iniciais(): string {
    const nome = this.currentUser.nome || this.currentUser.username;
    const partes = nome.trim().split(/\s+/).filter(Boolean);
    if (partes.length === 0) {
      return '?';
    }
    if (partes.length === 1) {
      return partes[0].slice(0, 2).toUpperCase();
    }
    return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
  }

  logout(): void {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
