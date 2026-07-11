package br.com.desafio.tecnico.gestao.security.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exemplo funcional de RBAC puro (specs/003, seção 2) - não usa /actuator/health para
 * não acoplar autenticação a health checks usados por orquestração/Prometheus (spec 002).
 */
@RestController
public class ExemploAutorizacaoController {

	@GetMapping("/api/exemplo/staff")
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public String areaStaff() {
		return "Acesso permitido: papel SECRETARIA ou ADMIN.";
	}

}
