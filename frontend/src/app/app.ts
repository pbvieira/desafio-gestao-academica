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

  logout(): void {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
