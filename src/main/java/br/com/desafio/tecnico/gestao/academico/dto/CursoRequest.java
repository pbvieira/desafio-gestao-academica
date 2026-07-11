package br.com.desafio.tecnico.gestao.academico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CursoRequest(

		@NotBlank(message = "código é obrigatório") @Size(max = 20, message = "código deve ter no máximo 20 caracteres") String codigo,

		@NotBlank(message = "nome é obrigatório") @Size(max = 200, message = "nome deve ter no máximo 200 caracteres") String nome) {
}
