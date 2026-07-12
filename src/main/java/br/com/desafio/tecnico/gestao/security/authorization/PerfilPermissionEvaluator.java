package br.com.desafio.tecnico.gestao.security.authorization;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import br.com.desafio.tecnico.gestao.academico.AlunoOwnershipResolver;

/**
 * Mecanismo de ABAC deste projeto (D012 em docs/DECISIONS.md): regra de posse "usuário
 * só vê o próprio recurso", com override para SECRETARIA/ADMIN. PERFIL é o exemplo
 * funcional da Fase 1 (specs/003, compara direto com o path variable, que já É o
 * subject). ALUNO/MATRICULA (D030, specs/006) resolvem o dono real via
 * AlunoOwnershipResolver - API pública do módulo `academico` (pacote raiz), não seus
 * repositórios internos, para não violar o encapsulamento de módulo do Spring
 * Modulith (ver ModularidadeTest).
 */
@Component
public class PerfilPermissionEvaluator implements PermissionEvaluator {

	private static final String TARGET_TYPE_PERFIL = "PERFIL";
	private static final String TARGET_TYPE_ALUNO = "ALUNO";
	private static final String TARGET_TYPE_MATRICULA = "MATRICULA";
	private static final String PERMISSION_READ = "READ";
	private static final String PERMISSION_MATRICULAR = "MATRICULAR";
	private static final String PERMISSION_GERENCIAR = "GERENCIAR";
	private static final String ROLE_SECRETARIA = "ROLE_SECRETARIA";
	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private final AlunoOwnershipResolver ownershipResolver;

	public PerfilPermissionEvaluator(AlunoOwnershipResolver ownershipResolver) {
		this.ownershipResolver = ownershipResolver;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		// Não utilizado nesta fase - o exemplo funcional dispatcha por targetId/targetType.
		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
			Object permission) {
		if (targetId == null || targetType == null) {
			return false;
		}
		return switch (targetType) {
			case TARGET_TYPE_PERFIL -> permitirPerfil(authentication, targetId, permission);
			case TARGET_TYPE_ALUNO -> permitirAluno(authentication, targetId, permission);
			case TARGET_TYPE_MATRICULA -> permitirMatricula(authentication, targetId, permission);
			default -> false;
		};
	}

	private boolean permitirPerfil(Authentication authentication, Serializable targetId, Object permission) {
		// Só "READ" é uma permissão definida para PERFIL nesta fase - qualquer outra
		// (ex: "WRITE") nega por padrão em vez de se comportar como READ silenciosamente
		// (achado de code review: o parâmetro "permission" era recebido mas ignorado).
		if (!PERMISSION_READ.equals(permission)) {
			return false;
		}
		if (isStaff(authentication)) {
			return true;
		}
		String subject = subjectOf(authentication);
		return subject != null && subject.equals(String.valueOf(targetId));
	}

	private boolean permitirAluno(Authentication authentication, Serializable targetId, Object permission) {
		if (!PERMISSION_MATRICULAR.equals(permission)) {
			return false;
		}
		if (isStaff(authentication)) {
			return true;
		}
		Long alunoId = comoLong(targetId);
		if (alunoId == null) {
			return false;
		}
		String subject = subjectOf(authentication);
		return subject != null
				&& ownershipResolver.keycloakSubjectIdDoAluno(alunoId).map(subject::equals).orElse(false);
	}

	private boolean permitirMatricula(Authentication authentication, Serializable targetId, Object permission) {
		if (!PERMISSION_GERENCIAR.equals(permission)) {
			return false;
		}
		if (isStaff(authentication)) {
			return true;
		}
		Long matriculaId = comoLong(targetId);
		if (matriculaId == null) {
			return false;
		}
		String subject = subjectOf(authentication);
		return subject != null && ownershipResolver.keycloakSubjectIdDoAlunoDaMatricula(matriculaId)
				.map(subject::equals).orElse(false);
	}

	private Long comoLong(Serializable targetId) {
		if (targetId instanceof Long l) {
			return l;
		}
		if (targetId instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(targetId));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private boolean isStaff(Authentication authentication) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (ROLE_SECRETARIA.equals(authority.getAuthority()) || ROLE_ADMIN.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	private String subjectOf(Authentication authentication) {
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return null;
	}

}
