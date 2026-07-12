import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ErrorBannerService {
  readonly mensagem = signal<string | null>(null);

  show(mensagem: string): void {
    this.mensagem.set(mensagem);
  }

  clear(): void {
    this.mensagem.set(null);
  }
}
