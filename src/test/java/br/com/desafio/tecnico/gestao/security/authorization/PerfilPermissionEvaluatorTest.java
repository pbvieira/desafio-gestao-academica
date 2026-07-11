package br.com.desafio.tecnico.gestao.security.authorization;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilPermissionEvaluatorTest {

	private final PerfilPermissionEvaluator evaluator = new PerfilPermissionEvaluator();

	@Test
	void donoDoPerfilTemPermissao() {
		var authentication = tokenPara("user-a", "ROLE_ALUNO");

		assertThat(evaluator.hasPermission(authentication, "user-a", "PERFIL", "READ")).isTrue();
	}

	@Test
	void outroUsuarioNaoStaffNaoTemPermissao() {
		var authentication = tokenPara("user-a", "ROLE_ALUNO");

		assertThat(evaluator.hasPermission(authentication, "user-b", "PERFIL", "READ")).isFalse();
	}

	@Test
	void secretariaTemPermissaoSobrePerfilAlheio() {
		var authentication = tokenPara("user-staff", "ROLE_SECRETARIA");

		assertThat(evaluator.hasPermission(authentication, "user-b", "PERFIL", "READ")).isTrue();
	}

	@Test
	void adminTemPermissaoSobrePerfilAlheio() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, "user-b", "PERFIL", "READ")).isTrue();
	}

	@Test
	void targetTypeDiferenteDePerfilNuncaTemPermissao() {
		var authentication = tokenPara("user-a", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, "user-a", "OUTRO_TIPO", "READ")).isFalse();
	}

	@Test
	void permissaoDiferenteDeReadNuncaTemPermissao() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, "user-a", "PERFIL", "WRITE")).isFalse();
	}

	@Test
	void overloadDeObjetoDeDominioNaoUtilizadoSempreNega() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, new Object(), "READ")).isFalse();
	}

	private JwtAuthenticationToken tokenPara(String subject, String authority) {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(subject)
				.claim("sub", subject)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(authority)));
	}

}
