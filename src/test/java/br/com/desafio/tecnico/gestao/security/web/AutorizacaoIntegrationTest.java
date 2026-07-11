package br.com.desafio.tecnico.gestao.security.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modelo de teste para RBAC e ABAC (specs/003, seção 7) - reutiliza JWT simulado
 * (Spring Security Test), sem depender de um Keycloak real; a Fase 2 reaproveita este
 * mesmo padrão para as regras ABAC reais (Matrícula/Turma).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacaoIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rbacSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/api/exemplo/staff"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rbacComTokenSemPapelCorretoRetorna403() throws Exception {
		mockMvc.perform(get("/api/exemplo/staff").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUNO"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void rbacComPapelCorretoRetorna200() throws Exception {
		mockMvc.perform(
				get("/api/exemplo/staff").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"))))
				.andExpect(status().isOk());
	}

	@Test
	void abacSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/api/usuarios/user-a/perfil"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void abacDonoDoPerfilRetorna200() throws Exception {
		mockMvc.perform(get("/api/usuarios/user-a/perfil")
				.with(jwt().jwt(j -> j.subject("user-a")).authorities(new SimpleGrantedAuthority("ROLE_ALUNO"))))
				.andExpect(status().isOk());
	}

	@Test
	void abacPerfilAlheioSemStaffRetorna403() throws Exception {
		mockMvc.perform(get("/api/usuarios/user-b/perfil")
				.with(jwt().jwt(j -> j.subject("user-a")).authorities(new SimpleGrantedAuthority("ROLE_ALUNO"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void abacPerfilAlheioComAdminRetorna200() throws Exception {
		mockMvc.perform(get("/api/usuarios/user-b/perfil")
				.with(jwt().jwt(j -> j.subject("user-admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk());
	}

	@Test
	void abacPerfilAlheioComSecretariaRetorna200() throws Exception {
		mockMvc.perform(get("/api/usuarios/user-b/perfil").with(
				jwt().jwt(j -> j.subject("user-staff")).authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"))))
				.andExpect(status().isOk());
	}

}
