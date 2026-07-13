package br.com.desafio.tecnico.gestao.administracao.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean {@link Keycloak} (cliente admin) autenticado via {@code client_credentials} no client
 * confidential {@code gestao-backend}, consumido por
 * {@link br.com.desafio.tecnico.gestao.administracao.service.AdministracaoUsuarioService} para falar
 * com a Admin API do Keycloak (specs/010, D045/D046) - requer {@code serviceAccountsEnabled: true} e
 * os client roles {@code view-users}/{@code manage-users}/{@code query-users}/{@code view-realm} de
 * {@code realm-management} atribuídos ao service account desse client
 * ({@code docker/keycloak/import/gestao-realm.json}).
 */
@Configuration
public class KeycloakAdminConfig {

	@Bean
	Keycloak keycloakAdmin(@Value("${gestao.keycloak-admin.server-url}") String serverUrl,
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
