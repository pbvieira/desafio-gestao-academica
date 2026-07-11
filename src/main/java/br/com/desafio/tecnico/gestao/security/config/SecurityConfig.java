package br.com.desafio.tecnico.gestao.security.config;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import br.com.desafio.tecnico.gestao.security.support.KeycloakClaims;

/**
 * RBAC via Keycloak (specs/003-seguranca-rbac-abac.md): a aplicação é um OAuth2
 * Resource Server (D010) - valida a assinatura/expiração de Bearer tokens via JWKS do
 * realm (issuer-uri em application.properties), sem manter sessão nem iniciar login.
 * CSRF desabilitado e sessão STATELESS - achado de security review: um resource server
 * puro de Bearer token não usa cookie de sessão, então proteção CSRF (pensada para
 * credenciais ambiente tipo cookie) não se aplica e só atrapalharia métodos de escrita
 * futuros (POST/PUT/DELETE da Fase 2).
 */
@Configuration
public class SecurityConfig {

	/**
	 * Ao declarar SecurityFilterChain(s) próprios, o Spring Boot não cria mais o
	 * UserDetailsService default a partir de spring.security.user.* - precisa ser
	 * explícito para o Basic Auth do scrape do Prometheus (abaixo) ter alguém contra
	 * quem autenticar.
	 */
	@Bean
	UserDetailsService prometheusScrapeUserDetailsService(
			@Value("${spring.security.user.name}") String username,
			@Value("${spring.security.user.password}") String password, PasswordEncoder passwordEncoder) {
		var user = User.withUsername(username).password(passwordEncoder.encode(password)).roles("PROMETHEUS").build();
		return new InMemoryUserDetailsManager(user);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/**
	 * Cadeia dedicada para o scrape do Prometheus (specs/002), com Basic Auth próprio -
	 * achado de security review (H1): antes, /actuator/prometheus era público, expondo
	 * padrões de tráfego/latência da API sem nenhuma credencial. Precisa vir ANTES da
	 * cadeia principal (@Order menor), que trata todo o resto via JWT.
	 */
	@Bean
	@Order(1)
	SecurityFilterChain actuatorPrometheusSecurityFilterChain(HttpSecurity http,
			AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		http.securityMatcher("/actuator/prometheus")
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				// Reaproveita o mesmo AuthenticationEntryPoint (ProblemDetail via
				// ProblemDetailFactory, D016) do resto da API - achado de code review:
				// sem isto, uma falha de Basic Auth aqui usava o formato default do
				// Spring Security, diferente do resto da API.
				.httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint,
			AccessDeniedHandler accessDeniedHandler) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}

	/**
	 * O conversor default do Spring Security não sabe extrair papéis do formato que o
	 * Keycloak usa (claim "realm_access.roles") - sem isto, hasRole(...)/hasAuthority(...)
	 * nunca bateriam com os papéis ALUNO/SECRETARIA/ADMIN do realm (D011).
	 */
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		var converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(this::realmRolesAsAuthorities);
		return converter;
	}

	private Collection<GrantedAuthority> realmRolesAsAuthorities(Jwt jwt) {
		return KeycloakClaims.realmRoles(jwt).stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.collect(Collectors.toUnmodifiableList());
	}

}
