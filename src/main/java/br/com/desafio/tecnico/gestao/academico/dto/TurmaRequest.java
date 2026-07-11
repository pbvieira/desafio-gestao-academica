package br.com.desafio.tecnico.gestao.academico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TurmaRequest(

		@NotBlank(message = "código é obrigatório") @Size(max = 20, message = "código deve ter no máximo 20 caracteres") String codigo,

		@NotNull(message = "cursoId é obrigatório") Long cursoId,

		@NotNull(message = "disciplinaId é obrigatório") Long disciplinaId,

		@NotNull(message = "limiteVagas é obrigatório") @Positive(message = "limiteVagas deve ser maior que zero") Integer limiteVagas) {
}
