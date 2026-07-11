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

import br.com.desafio.tecnico.gestao.academico.dto.DisciplinaRequest;
import br.com.desafio.tecnico.gestao.academico.dto.DisciplinaResponse;
import br.com.desafio.tecnico.gestao.academico.service.DisciplinaService;

/**
 * Leitura aberta a qualquer autenticado; escrita restrita a SECRETARIA/ADMIN (D021).
 */
@RestController
@RequestMapping("/api/disciplinas")
@Tag(name = "Disciplinas")
public class DisciplinaController {

	private final DisciplinaService disciplinaService;

	public DisciplinaController(DisciplinaService disciplinaService) {
		this.disciplinaService = disciplinaService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public DisciplinaResponse criar(@Valid @RequestBody DisciplinaRequest request) {
		return DisciplinaResponse.de(disciplinaService.criar(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public DisciplinaResponse editar(@PathVariable Long id, @Valid @RequestBody DisciplinaRequest request) {
		return DisciplinaResponse.de(disciplinaService.editar(id, request));
	}

	@GetMapping
	public List<DisciplinaResponse> listar() {
		return disciplinaService.listar().stream().map(DisciplinaResponse::de).toList();
	}

	@GetMapping("/{id}")
	public DisciplinaResponse buscar(@PathVariable Long id) {
		return DisciplinaResponse.de(disciplinaService.buscarAtivo(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public void excluir(@PathVariable Long id) {
		disciplinaService.excluir(id);
	}

}
