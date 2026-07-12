package br.com.desafio.tecnico.gestao.academico.dto;

import jakarta.validation.constraints.NotNull;

public record MatriculaRequest(

		@NotNull(message = "alunoId é obrigatório") Long alunoId,

		@NotNull(message = "turmaId é obrigatório") Long turmaId) {
}
