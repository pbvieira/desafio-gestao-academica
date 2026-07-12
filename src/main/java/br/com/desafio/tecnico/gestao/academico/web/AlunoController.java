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
import br.com.desafio.tecnico.gestao.academico.dto.MatriculaResponse;
import br.com.desafio.tecnico.gestao.academico.service.AlunoService;
import br.com.desafio.tecnico.gestao.academico.service.MatriculaService;

/**
 * CRUD de Aluno (criar/editar/listar/buscar/excluir) restrito a SECRETARIA/ADMIN
 * (@PreAuthorize de classe, specs/005 seção 4.4) - consultar/listar dados de outros
 * alunos não é algo que um ALUNO deveria poder fazer livremente. listarMatriculas é a
 * exceção: sobrescreve a restrição de classe com ABAC (specs/006, D030) - primeiro uso
 * real do vínculo Keycloak<->Aluno (D006/D030).
 */
@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos")
@PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN')")
public class AlunoController {

	private final AlunoService alunoService;
	private final MatriculaService matriculaService;

	public AlunoController(AlunoService alunoService, MatriculaService matriculaService) {
		this.alunoService = alunoService;
		this.matriculaService = matriculaService;
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

	/**
	 * specs/006-matricula.md, seção 4.5: sobrescreve o @PreAuthorize de classe
	 * (staff-only) - aluno pode consultar as próprias matrículas (ABAC, D030),
	 * SECRETARIA/ADMIN consultam qualquer uma.
	 */
	@GetMapping("/{alunoId}/matriculas")
	@PreAuthorize("hasPermission(#alunoId, 'ALUNO', 'MATRICULAR')")
	public List<MatriculaResponse> listarMatriculas(@PathVariable Long alunoId) {
		return matriculaService.listarPorAluno(alunoId).stream().map(MatriculaResponse::de).toList();
	}

}
