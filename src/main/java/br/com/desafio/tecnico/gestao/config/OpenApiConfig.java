package br.com.desafio.tecnico.gestao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Esquema de segurança Bearer JWT no Swagger UI (specs/004, seção 4.5) - habilita o
 * botão "Authorize" para colar um token do Keycloak e reproduzir os bloqueios de
 * RBAC/ABAC (spec 003) direto na UI, aplicado globalmente (não endpoint por endpoint).
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME_NAME = "bearerAuth";

	@Bean
	OpenAPI gestaoOpenApi() {
		return new OpenAPI()
				.components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
	}

}
