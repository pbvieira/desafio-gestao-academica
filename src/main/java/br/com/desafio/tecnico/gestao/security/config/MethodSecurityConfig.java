package br.com.desafio.tecnico.gestao.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Habilita @PreAuthorize com hasPermission(...) usando o PermissionEvaluator
 * customizado (D012 em docs/DECISIONS.md) - mecanismo de ABAC deste projeto.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

	@Bean
	MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
		var handler = new DefaultMethodSecurityExpressionHandler();
		handler.setPermissionEvaluator(permissionEvaluator);
		return handler;
	}

}
