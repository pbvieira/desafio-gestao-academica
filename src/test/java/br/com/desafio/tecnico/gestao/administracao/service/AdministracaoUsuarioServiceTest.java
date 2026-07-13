package br.com.desafio.tecnico.gestao.administracao.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.tecnico.gestao.administracao.dto.Papel;
import br.com.desafio.tecnico.gestao.administracao.dto.UsuarioAdminDto;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministracaoUsuarioServiceTest {

	private static final String REALM = "gestao";

	@Mock
	private Keycloak keycloak;
	@Mock
	private RealmResource realmResource;
	@Mock
	private UsersResource usersResource;
	@Mock
	private UserResource userResource;
	@Mock
	private RoleMappingResource roleMappingResource;
	@Mock
	private RoleScopeResource roleScopeResource;
	@Mock
	private RolesResource rolesResource;
	@Mock
	private RoleResource roleResourceAluno;
	@Mock
	private RoleResource roleResourceSecretaria;
	@Mock
	private RoleResource roleResourceAdmin;

	private AdministracaoUsuarioService service;

	@org.junit.jupiter.api.BeforeEach
	void configurarService() {
		service = new AdministracaoUsuarioService(keycloak, REALM);
		when(keycloak.realm(REALM)).thenReturn(realmResource);
		when(realmResource.users()).thenReturn(usersResource);
	}

	/**
	 * D048: listarUsuarios() resolve papel via membros das 3 roles gerenciadas
	 * (getUserMembers(), O(3)), não mais uma chamada de roles por usuário (N+1).
	 * Por padrão nenhuma role tem membros - os testes sobrescrevem a role relevante.
	 */
	private void configurarRolesGerenciadasSemMembros() {
		when(realmResource.roles()).thenReturn(rolesResource);
		when(rolesResource.get("ALUNO")).thenReturn(roleResourceAluno);
		when(rolesResource.get("SECRETARIA")).thenReturn(roleResourceSecretaria);
		when(rolesResource.get("ADMIN")).thenReturn(roleResourceAdmin);
		when(roleResourceAluno.getUserMembers()).thenReturn(List.of());
		when(roleResourceSecretaria.getUserMembers()).thenReturn(List.of());
		when(roleResourceAdmin.getUserMembers()).thenReturn(List.of());
	}

	@Test
	void listarUsuarios_mapeiaUsuarioComPapelRealmRole() {
		UserRepresentation representacao = new UserRepresentation();
		representacao.setId("kc-1");
		representacao.setUsername("aluno.teste");
		representacao.setFirstName("Aluno");
		representacao.setLastName("Teste");
		representacao.setEmail("aluno.teste@gestao.local");
		when(usersResource.list()).thenReturn(List.of(representacao));
		configurarRolesGerenciadasSemMembros();
		when(roleResourceAluno.getUserMembers()).thenReturn(List.of(representacao));

		List<UsuarioAdminDto> usuarios = service.listarUsuarios();

		assertThat(usuarios).hasSize(1);
		UsuarioAdminDto usuario = usuarios.get(0);
		assertThat(usuario.id()).isEqualTo("kc-1");
		assertThat(usuario.username()).isEqualTo("aluno.teste");
		assertThat(usuario.nome()).isEqualTo("Aluno Teste");
		assertThat(usuario.email()).isEqualTo("aluno.teste@gestao.local");
		assertThat(usuario.papel()).isEqualTo("ALUNO");
	}

	@Test
	void listarUsuarios_usuarioSemPapelGerenciado_papelNulo() {
		UserRepresentation representacao = new UserRepresentation();
		representacao.setId("kc-2");
		representacao.setUsername("sem.papel");
		representacao.setFirstName("Sem");
		representacao.setLastName("Papel");
		representacao.setEmail("sem.papel@gestao.local");
		when(usersResource.list()).thenReturn(List.of(representacao));
		configurarRolesGerenciadasSemMembros();

		List<UsuarioAdminDto> usuarios = service.listarUsuarios();

		assertThat(usuarios).hasSize(1);
		UsuarioAdminDto usuario = usuarios.get(0);
		assertThat(usuario.id()).isEqualTo("kc-2");
		assertThat(usuario.papel()).isNull();
	}

	@Test
	void reatribuirPapel_removeOPapelAtualEAdicionaONovo() {
		when(usersResource.get("kc-1")).thenReturn(userResource);
		UserRepresentation representacao = new UserRepresentation();
		representacao.setId("kc-1");
		when(userResource.toRepresentation()).thenReturn(representacao);
		when(userResource.roles()).thenReturn(roleMappingResource);
		when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
		RoleRepresentation papelAtual = new RoleRepresentation();
		papelAtual.setName("ALUNO");
		when(roleScopeResource.listAll()).thenReturn(List.of(papelAtual));
		when(realmResource.roles()).thenReturn(rolesResource);
		when(rolesResource.get("ADMIN")).thenReturn(roleResourceAdmin);
		RoleRepresentation papelNovo = new RoleRepresentation();
		papelNovo.setName("ADMIN");
		when(roleResourceAdmin.toRepresentation()).thenReturn(papelNovo);

		service.reatribuirPapel("kc-1", Papel.ADMIN);

		verify(roleScopeResource).remove(List.of(papelAtual));
		verify(roleScopeResource).add(List.of(papelNovo));
	}

	@Test
	void reatribuirPapel_usuarioInexistente_lancaRecursoNaoEncontrado() {
		when(usersResource.get("kc-inexistente")).thenReturn(userResource);
		when(userResource.toRepresentation())
				.thenThrow(new jakarta.ws.rs.NotFoundException());

		assertThatThrownBy(() -> service.reatribuirPapel("kc-inexistente", Papel.ADMIN))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
