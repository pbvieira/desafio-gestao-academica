import { Injectable, inject } from '@angular/core';
import Keycloak from 'keycloak-js';

export type Papel = 'ALUNO' | 'SECRETARIA' | 'ADMIN';

/**
 * Claims lidas diretamente do token decodificado (tokenParsed) - sem chamada ao backend.
 * D040: "Meu Perfil" do ALUNO usa este mesmo mecanismo, já que o AlunoController não
 * expõe leitura do próprio registro para o papel ALUNO.
 */
@Injectable({ providedIn: 'root' })
export class CurrentUser {
  private readonly keycloak = inject(Keycloak);

  get nome(): string {
    return (this.keycloak.tokenParsed?.['name'] as string) ?? '';
  }

  get email(): string {
    return (this.keycloak.tokenParsed?.['email'] as string) ?? '';
  }

  get username(): string {
    return (this.keycloak.tokenParsed?.['preferred_username'] as string) ?? '';
  }

  get papeis(): Papel[] {
    const roles = (this.keycloak.realmAccess?.roles ?? []) as string[];
    return roles.filter((r): r is Papel => r === 'ALUNO' || r === 'SECRETARIA' || r === 'ADMIN');
  }

  temPapel(...papeis: Papel[]): boolean {
    return this.papeis.some((p) => papeis.includes(p));
  }

  get ehSecretariaOuAdmin(): boolean {
    return this.temPapel('SECRETARIA', 'ADMIN');
  }
}
