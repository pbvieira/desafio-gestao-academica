package br.com.desafio.tecnico.gestao.security.authorization;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Mecanismo de ABAC deste projeto (D012 em docs/DECISIONS.md): regra de posse "usuário
 * só vê o próprio perfil", com override para SECRETARIA/ADMIN. Este é o exemplo
 * funcional da Fase 1 (specs/003) sobre um recurso que já existe (perfil do usuário
 * autenticado) - a Fase 2 reaproveita o mesmo padrão para Matrícula/Turma, resolvendo o
 * dono real via repositório em vez de comparar direto com o path variable como aqui
 * (ver specs/003-seguranca-rbac-abac.md, seção 4.3).
 */
@Component
public class PerfilPermissionEvaluator implements PermissionEvaluator {

	private static final String TARGET_TYPE_PERFIL = "PERFIL";
	private static final String PERMISSION_READ = "READ";
	private static final String ROLE_SECRETARIA = "ROLE_SECRETARIA";
	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		// Não utilizado nesta fase - o exemplo funcional dispatcha por targetId/targetType.
		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
			Object permission) {
		if (!TARGET_TYPE_PERFIL.equals(targetType)) {
			return false;
		}
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
