package br.com.desafio.tecnico.gestao.academico.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.desafio.tecnico.gestao.academico.dto.MatriculaRequest;
import br.com.desafio.tecnico.gestao.academico.dto.MatriculaResponse;
import br.com.desafio.tecnico.gestao.academico.service.MatriculaService;

/**
 * specs/006-matricula.md, seção 4.5. RBAC/ABAC: criar/cancelar/consultar usam ABAC
 * (D030 - aluno só na própria, staff em qualquer uma); confirmar é RBAC puro,
 * SECRETARIA/ADMIN (D027) - PENDENTE->CONFIRMADA é um gate administrativo, não
 * self-service.
 */
@RestController
@RequestMapping("/api/matriculas")
@Tag(name = "Matrículas")
public class MatriculaController {

	private final MatriculaService matriculaService;

	public MatriculaController(MatriculaService matriculaService) {
		this.matriculaService = matriculaService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasPermission(#request.alunoId(), 'ALUNO', 'MATRICULAR')")
	public MatriculaResponse criar(@Valid @RequestBody MatriculaRequest request) {
		return MatriculaResponse.de(matriculaService.criar(request));
	}

	@PostMapping("/{id}/confirmar")
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public MatriculaResponse confirmar(@PathVariable Long id) {
		return MatriculaResponse.de(matriculaService.confirmar(id));
	}

	@PostMapping("/{id}/cancelar")
	@PreAuthorize("hasPermission(#id, 'MATRICULA', 'GERENCIAR')")
	public MatriculaResponse cancelar(@PathVariable Long id) {
		return MatriculaResponse.de(matriculaService.cancelar(id));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasPermission(#id, 'MATRICULA', 'GERENCIAR')")
	public MatriculaResponse buscar(@PathVariable Long id) {
		return MatriculaResponse.de(matriculaService.buscarPorId(id));
	}

}
