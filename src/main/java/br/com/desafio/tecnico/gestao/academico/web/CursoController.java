package br.com.desafio.tecnico.gestao.academico.web;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.desafio.tecnico.gestao.academico.dto.CursoRequest;
import br.com.desafio.tecnico.gestao.academico.dto.CursoResponse;
import br.com.desafio.tecnico.gestao.academico.service.CursoService;

/**
 * Leitura aberta a qualquer autenticado; escrita restrita a SECRETARIA/ADMIN (D021).
 */
@RestController
@RequestMapping("/api/cursos")
@Tag(name = "Cursos")
public class CursoController {

	private final CursoService cursoService;

	public CursoController(CursoService cursoService) {
		this.cursoService = cursoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public CursoResponse criar(@Valid @RequestBody CursoRequest request) {
		return CursoResponse.de(cursoService.criar(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public CursoResponse editar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
		return CursoResponse.de(cursoService.editar(id, request));
	}

	@GetMapping
	public List<CursoResponse> listar() {
		return cursoService.listar().stream().map(CursoResponse::de).toList();
	}

	@GetMapping("/{id}")
	public CursoResponse buscar(@PathVariable Long id) {
		return CursoResponse.de(cursoService.buscarAtivo(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public void excluir(@PathVariable Long id) {
		cursoService.excluir(id);
	}

	@PostMapping("/{id}/disciplinas/{disciplinaId}")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public CursoResponse vincularDisciplina(@PathVariable Long id, @PathVariable Long disciplinaId) {
		return CursoResponse.de(cursoService.vincularDisciplina(id, disciplinaId));
	}

	@DeleteMapping("/{id}/disciplinas/{disciplinaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public void desvincularDisciplina(@PathVariable Long id, @PathVariable Long disciplinaId) {
		cursoService.desvincularDisciplina(id, disciplinaId);
	}

}
