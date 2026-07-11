package br.com.desafio.tecnico.gestao.errorhandling;

/**
 * Reutilizável pela Fase 2 (ex: aluno já matriculado na turma). specs/004, seção 4.2.
 */
public class ConflitoRegraNegocioException extends RuntimeException {

	public ConflitoRegraNegocioException(String message) {
		super(message);
	}

}
