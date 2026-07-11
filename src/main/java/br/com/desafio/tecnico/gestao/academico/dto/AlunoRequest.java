package br.com.desafio.tecnico.gestao.academico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlunoRequest(

		@NotBlank(message = "nome é obrigatório") @Size(max = 200, message = "nome deve ter no máximo 200 caracteres") String nome,

		@NotBlank(message = "email é obrigatório") @Email(message = "email inválido") @Size(max = 200, message = "email deve ter no máximo 200 caracteres") String email) {
}
