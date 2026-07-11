package br.com.desafio.tecnico.gestao.security.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.desafio.tecnico.gestao.security.support.KeycloakClaims;

/**
 * Exemplo funcional de ABAC (specs/003, seção 2): usuário só vê o próprio perfil,
 * exceto SECRETARIA/ADMIN (override de staff) - ver PerfilPermissionEvaluator (D012).
 * {id} é o subject (sub) do Keycloak do usuário cujo perfil está sendo consultado.
 */
@RestController
public class PerfilController {

	@GetMapping("/api/usuarios/{id}/perfil")
	@PreAuthorize("hasPermission(#id, 'PERFIL', 'READ')")
	public PerfilResponse perfil(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
		return new PerfilResponse(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
				jwt.getClaimAsString("email"), KeycloakClaims.realmRoles(jwt));
	}

}
