package br.com.desafio.tecnico.gestao.errorhandling.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

/**
 * Endpoints placeholder demonstrando/testando o tratamento padronizado de erros
 * (specs/004, seção 2) - mesma linha dos exemplos de RBAC/ABAC da spec 003, não uma
 * entidade de domínio fictícia. "existe" é o único id/nome reconhecido de propósito.
 */
@RestController
@RequestMapping("/api/exemplo/recurso")
public class ExemploRecursoController {

	private static final String IDENTIFICADOR_EXISTENTE = "existe";

	@GetMapping("/{id}")
	public String buscar(@PathVariable String id) {
		if (!IDENTIFICADOR_EXISTENTE.equals(id)) {
			throw new RecursoNaoEncontradoException("Recurso com id '" + id + "' não encontrado.");
		}
		return "Recurso encontrado: " + id;
	}

	@PostMapping
	public String criar(@Valid @RequestBody ExemploRecursoRequest request) {
		if (IDENTIFICADOR_EXISTENTE.equals(request.nome())) {
			throw new ConflitoRegraNegocioException("Recurso com nome '" + request.nome() + "' já existe.");
		}
		return "Recurso criado: " + request.nome();
	}

}
