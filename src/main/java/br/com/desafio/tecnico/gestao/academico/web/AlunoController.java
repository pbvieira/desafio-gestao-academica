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

import br.com.desafio.tecnico.gestao.academico.dto.AlunoRequest;
import br.com.desafio.tecnico.gestao.academico.dto.AlunoResponse;
import br.com.desafio.tecnico.gestao.academico.service.AlunoService;

/**
 * Todas as operações (incluindo leitura) restritas a SECRETARIA/ADMIN nesta fase -
 * consultar/listar dados de outros alunos não é algo que um ALUNO deveria poder fazer
 * livremente; self-view fica para quando o vínculo Keycloak<->Aluno for exercitado de
 * verdade (specs/005, seção 4.4).
 */
@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos")
@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
public class AlunoController {

	private final AlunoService alunoService;

	public AlunoController(AlunoService alunoService) {
		this.alunoService = alunoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AlunoResponse criar(@Valid @RequestBody AlunoRequest request) {
		return AlunoResponse.de(alunoService.criar(request));
	}

	@PutMapping("/{id}")
	public AlunoResponse editar(@PathVariable Long id, @Valid @RequestBody AlunoRequest request) {
		return AlunoResponse.de(alunoService.editar(id, request));
	}

	@GetMapping
	public List<AlunoResponse> listar() {
		return alunoService.listar().stream().map(AlunoResponse::de).toList();
	}

	@GetMapping("/{id}")
	public AlunoResponse buscar(@PathVariable Long id) {
		return AlunoResponse.de(alunoService.buscarAtivo(id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		alunoService.excluir(id);
	}

}
