package br.com.desafio.tecnico.gestao.security.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakClaimsTest {

	@Test
	void semClaimRealmAccessDevolveListaVazia() {
		Jwt jwt = jwtSemRealmAccess();

		assertThat(KeycloakClaims.realmRoles(jwt)).isEmpty();
	}

	@Test
	void realmAccessSemChaveRolesDevolveListaVazia() {
		Jwt jwt = jwtComRealmAccess(Map.of("outraChave", "valor"));

		assertThat(KeycloakClaims.realmRoles(jwt)).isEmpty();
	}

	@Test
	void realmAccessComRolesDevolveOsPapeis() {
		Jwt jwt = jwtComRealmAccess(Map.of("roles", List.of("ALUNO", "ADMIN")));

		assertThat(KeycloakClaims.realmRoles(jwt)).containsExactly("ALUNO", "ADMIN");
	}

	private Jwt jwtSemRealmAccess() {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user-a")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
	}

	private Jwt jwtComRealmAccess(Map<String, Object> realmAccess) {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user-a")
				.claim("realm_access", realmAccess)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
	}

}
