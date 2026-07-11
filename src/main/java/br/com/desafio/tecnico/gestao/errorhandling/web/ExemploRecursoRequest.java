package br.com.desafio.tecnico.gestao.errorhandling.web;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de exemplo só para demonstrar/testar o 400 (validação) e o 409 (conflito) do
 * GlobalExceptionHandler (specs/004) - não é uma entidade de domínio.
 */
public record ExemploRecursoRequest(@NotBlank String nome) {
}
