package br.com.desafio.tecnico.gestao.academico.dto;

import java.time.Instant;

import br.com.desafio.tecnico.gestao.academico.domain.Curso;

public record CursoResponse(Long id, String codigo, String nome, boolean ativo, Instant criadoEm) {

	public static CursoResponse de(Curso curso) {
		return new CursoResponse(curso.getId(), curso.getCodigo(), curso.getNome(), curso.isAtivo(),
				curso.getCriadoEm());
	}

}
