package br.com.desafio.tecnico.gestao.administracao.web;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.desafio.tecnico.gestao.administracao.dto.ReatribuirPapelRequest;
import br.com.desafio.tecnico.gestao.administracao.dto.UsuarioAdminDto;
import br.com.desafio.tecnico.gestao.administracao.service.AdministracaoUsuarioService;

/**
 * specs/010: ADMIN-only, sem excecão de método (D045) - gerenciar quem tem qual papel é
 * justamente a primeira regra real que diferencia ADMIN de SECRETARIA (D011).
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "Administração")
@PreAuthorize("hasRole('ADMIN')")
public class AdministracaoUsuarioController {

	private final AdministracaoUsuarioService administracaoUsuarioService;

	public AdministracaoUsuarioController(AdministracaoUsuarioService administracaoUsuarioService) {
		this.administracaoUsuarioService = administracaoUsuarioService;
	}

	@GetMapping
	public List<UsuarioAdminDto> listar() {
		return administracaoUsuarioService.listarUsuarios();
	}

	@PatchMapping("/{id}/papel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reatribuirPapel(@PathVariable String id, @Valid @RequestBody ReatribuirPapelRequest request) {
		administracaoUsuarioService.reatribuirPapel(id, request.papel());
	}

}
