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

import br.com.desafio.tecnico.gestao.academico.dto.TurmaRequest;
import br.com.desafio.tecnico.gestao.academico.dto.TurmaResponse;
import br.com.desafio.tecnico.gestao.academico.service.TurmaService;

/**
 * Leitura aberta a qualquer autenticado; escrita restrita a SECRETARIA/ADMIN (D021).
 * Nenhuma lógica de vaga/concorrência aqui - fora de escopo desta fase.
 */
@RestController
@RequestMapping("/api/turmas")
@Tag(name = "Turmas")
public class TurmaController {

	private final TurmaService turmaService;

	public TurmaController(TurmaService turmaService) {
		this.turmaService = turmaService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public TurmaResponse criar(@Valid @RequestBody TurmaRequest request) {
		return TurmaResponse.de(turmaService.criar(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public TurmaResponse editar(@PathVariable Long id, @Valid @RequestBody TurmaRequest request) {
		return TurmaResponse.de(turmaService.editar(id, request));
	}

	@GetMapping
	public List<TurmaResponse> listar() {
		return turmaService.listar().stream().map(TurmaResponse::de).toList();
	}

	@GetMapping("/{id}")
	public TurmaResponse buscar(@PathVariable Long id) {
		return TurmaResponse.de(turmaService.buscarAtiva(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
	public void excluir(@PathVariable Long id) {
		turmaService.excluir(id);
	}

}
