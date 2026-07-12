package br.com.desafio.tecnico.gestao.errorhandling;

/**
 * Reutilizável pela Fase 2 (ex: aluno já matriculado na turma). specs/004, seção 4.2.
 * errorCode default "BUSINESS_CONFLICT" preserva o comportamento de todo uso existente
 * (Fase 2); specs/006 (Matrícula) usa o construtor com errorCode explícito
 * (VAGAS_ESGOTADAS, CONFLITO_CONCORRENCIA, TRANSICAO_INVALIDA, MATRICULA_DUPLICADA) para
 * o cliente diferenciar motivos de 409 sem parsear a mensagem.
 */
public class ConflitoRegraNegocioException extends RuntimeException {

	private final String errorCode;

	public ConflitoRegraNegocioException(String message) {
		this(message, "BUSINESS_CONFLICT");
	}

	public ConflitoRegraNegocioException(String message, String errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

}
