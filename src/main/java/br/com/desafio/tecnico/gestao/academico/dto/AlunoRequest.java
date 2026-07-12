package br.com.desafio.tecnico.gestao.academico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * keycloakSubjectId (D030 em docs/DECISIONS.md): opcional, só SECRETARIA/ADMIN editam
 * Aluno (já era assim desde a Fase 2) - vincula o registro acadêmico à conta de login
 * no Keycloak, necessário para o ABAC de Matrícula (specs/006) funcionar de verdade.
 * Self-service de vínculo (o próprio aluno reivindicar seu registro) fica fora de
 * escopo.
 */
public record AlunoRequest(

		@NotBlank(message = "nome é obrigatório") @Size(max = 200, message = "nome deve ter no máximo 200 caracteres") String nome,

		@NotBlank(message = "email é obrigatório") @Email(message = "email inválido") @Size(max = 200, message = "email deve ter no máximo 200 caracteres") String email,

		@Size(max = 64, message = "keycloakSubjectId deve ter no máximo 64 caracteres") String keycloakSubjectId) {
}
