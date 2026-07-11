package br.com.desafio.tecnico.gestao.security.support;

import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Ponto único de leitura dos claims que o Keycloak coloca no JWT (specs/003) - evita
 * duas implementações divergentes do mesmo parsing (achado de code review).
 */
public final class KeycloakClaims {

	private KeycloakClaims() {
	}

	@SuppressWarnings("unchecked")
	public static List<String> realmRoles(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
			return List.of();
		}
		return (List<String>) roles;
	}

}
