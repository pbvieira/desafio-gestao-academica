package br.com.desafio.tecnico.gestao.errorhandling;

/**
 * Reutilizável pela Fase 2 (ex: Aluno/Curso/Turma não encontrado). specs/004, seção 4.2.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String message) {
		super(message);
	}

}
