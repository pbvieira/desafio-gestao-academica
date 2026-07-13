package br.com.desafio.tecnico.gestao.administracao.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

	@Bean
	public Keycloak keycloakAdmin(@Value("${gestao.keycloak-admin.server-url}") String serverUrl,
			@Value("${gestao.keycloak-admin.realm}") String realm,
			@Value("${gestao.keycloak-admin.client-id}") String clientId,
			@Value("${gestao.keycloak-admin.client-secret}") String clientSecret) {
		return KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.build();
	}

}
