package br.com.desafio.tecnico.gestao.administracao.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.desafio.tecnico.gestao.administracao.dto.Papel;
import br.com.desafio.tecnico.gestao.administracao.dto.UsuarioAdminDto;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

/**
 * specs/010: só reatribui papel de usuário já existente no Keycloak - não cria usuário,
 * não altera senha/enabled (D045). Um usuário só tem um dos três papéis gerenciados por
 * vez, por construção do realm (D045) - reatribuir remove o atual e adiciona o novo.
 */
@Service
public class AdministracaoUsuarioService {

	private static final Set<String> PAPEIS_GERENCIADOS = Set.of("ALUNO", "SECRETARIA", "ADMIN");

	private final Keycloak keycloakAdmin;
	private final String realm;

	public AdministracaoUsuarioService(Keycloak keycloakAdmin,
			@Value("${gestao.keycloak-admin.realm}") String realm) {
		this.keycloakAdmin = keycloakAdmin;
		this.realm = realm;
	}

	public List<UsuarioAdminDto> listarUsuarios() {
		RealmResource realmResource = keycloakAdmin.realm(realm);
		List<UserRepresentation> usuarios = realmResource.users().list();
		Map<String, String> papelPorUsuarioId = papelPorUsuarioId(realmResource);
		return usuarios.stream().map(usuario -> paraDto(usuario, papelPorUsuarioId.get(usuario.getId()))).toList();
	}

	/**
	 * D048: resolve o papel de todos os usuários listados com uma chamada por papel gerenciado
	 * (O(3)) em vez de uma chamada adicional por usuário (N+1).
	 */
	private Map<String, String> papelPorUsuarioId(RealmResource realmResource) {
		Map<String, String> papelPorUsuarioId = new HashMap<>();
		for (String papel : PAPEIS_GERENCIADOS) {
			List<UserRepresentation> membros = realmResource.roles().get(papel).getUserMembers();
			membros.forEach(membro -> papelPorUsuarioId.put(membro.getId(), papel));
		}
		return papelPorUsuarioId;
	}

	public void reatribuirPapel(String usuarioId, Papel novoPapel) {
		RealmResource realmResource = keycloakAdmin.realm(realm);
		UserResource usuario = buscarUsuario(realmResource.users(), usuarioId);

		RoleScopeResource papeisDoUsuario = usuario.roles().realmLevel();
		List<RoleRepresentation> papeisAtuais = papeisDoUsuario.listAll().stream()
				.filter(papel -> PAPEIS_GERENCIADOS.contains(papel.getName())).toList();
		if (!papeisAtuais.isEmpty()) {
			papeisDoUsuario.remove(papeisAtuais);
		}
		RoleRepresentation novoPapelRepresentacao = realmResource.roles().get(novoPapel.name()).toRepresentation();
		papeisDoUsuario.add(List.of(novoPapelRepresentacao));
	}

	private UserResource buscarUsuario(UsersResource usersResource, String usuarioId) {
		UserResource usuario = usersResource.get(usuarioId);
		try {
			usuario.toRepresentation();
		} catch (jakarta.ws.rs.NotFoundException e) {
			throw new RecursoNaoEncontradoException("Usuário com id '" + usuarioId + "' não encontrado.");
		}
		return usuario;
	}

	private UsuarioAdminDto paraDto(UserRepresentation usuario, String papel) {
		String nome = ((usuario.getFirstName() != null ? usuario.getFirstName() : "") + " "
				+ (usuario.getLastName() != null ? usuario.getLastName() : "")).trim();
		return new UsuarioAdminDto(usuario.getId(), usuario.getUsername(), nome, usuario.getEmail(), papel);
	}

}
