package br.com.desafio.tecnico.gestao.security.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.desafio.tecnico.gestao.academico.AlunoOwnershipResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPermissionEvaluatorTest {

	@Mock
	private AlunoOwnershipResolver ownershipResolver;

	@InjectMocks
	private PerfilPermissionEvaluator evaluator;

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
	void targetTypeDesconhecidoNuncaTemPermissao() {
		var authentication = tokenPara("user-a", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, "user-a", "OUTRO_TIPO", "READ")).isFalse();
	}

	@Test
	void permissaoDiferenteDeReadNuncaTemPermissaoParaPerfil() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, "user-a", "PERFIL", "WRITE")).isFalse();
	}

	@Test
	void overloadDeObjetoDeDominioNaoUtilizadoSempreNega() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, new Object(), "READ")).isFalse();
	}

	@Test
	void targetIdOuTargetTypeNuloNuncaTemPermissao() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, null, "PERFIL", "READ")).isFalse();
	}

	@Test
	void donoDoAlunoTemPermissaoParaMatricular() {
		var authentication = tokenPara("sub-1", "ROLE_ALUNO");
		when(ownershipResolver.keycloakSubjectIdDoAluno(1L)).thenReturn(Optional.of("sub-1"));

		assertThat(evaluator.hasPermission(authentication, 1L, "ALUNO", "MATRICULAR")).isTrue();
	}

	@Test
	void outroAlunoNaoTemPermissaoParaMatricularEmNomeDeOutro() {
		var authentication = tokenPara("sub-2", "ROLE_ALUNO");
		lenient().when(ownershipResolver.keycloakSubjectIdDoAluno(1L)).thenReturn(Optional.of("sub-1"));

		assertThat(evaluator.hasPermission(authentication, 1L, "ALUNO", "MATRICULAR")).isFalse();
	}

	@Test
	void secretariaTemPermissaoParaMatricularQualquerAluno() {
		var authentication = tokenPara("user-staff", "ROLE_SECRETARIA");

		assertThat(evaluator.hasPermission(authentication, 1L, "ALUNO", "MATRICULAR")).isTrue();
	}

	@Test
	void alunoInexistenteNuncaTemPermissao() {
		var authentication = tokenPara("sub-1", "ROLE_ALUNO");
		when(ownershipResolver.keycloakSubjectIdDoAluno(99L)).thenReturn(Optional.empty());

		assertThat(evaluator.hasPermission(authentication, 99L, "ALUNO", "MATRICULAR")).isFalse();
	}

	@Test
	void permissaoDiferenteDeMatricularNuncaTemPermissaoParaAluno() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, 1L, "ALUNO", "OUTRA")).isFalse();
	}

	@Test
	void donoDaMatriculaTemPermissaoParaGerenciar() {
		var authentication = tokenPara("sub-1", "ROLE_ALUNO");
		when(ownershipResolver.keycloakSubjectIdDoAlunoDaMatricula(5L)).thenReturn(Optional.of("sub-1"));

		assertThat(evaluator.hasPermission(authentication, 5L, "MATRICULA", "GERENCIAR")).isTrue();
	}

	@Test
	void outroAlunoNaoTemPermissaoSobreMatriculaAlheia() {
		var authentication = tokenPara("sub-2", "ROLE_ALUNO");
		lenient().when(ownershipResolver.keycloakSubjectIdDoAlunoDaMatricula(5L)).thenReturn(Optional.of("sub-1"));

		assertThat(evaluator.hasPermission(authentication, 5L, "MATRICULA", "GERENCIAR")).isFalse();
	}

	@Test
	void adminTemPermissaoSobreQualquerMatricula() {
		var authentication = tokenPara("user-admin", "ROLE_ADMIN");

		assertThat(evaluator.hasPermission(authentication, 5L, "MATRICULA", "GERENCIAR")).isTrue();
	}

	@Test
	void matriculaInexistenteNuncaTemPermissao() {
		var authentication = tokenPara("sub-1", "ROLE_ALUNO");
		when(ownershipResolver.keycloakSubjectIdDoAlunoDaMatricula(99L)).thenReturn(Optional.empty());

		assertThat(evaluator.hasPermission(authentication, 99L, "MATRICULA", "GERENCIAR")).isFalse();
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
