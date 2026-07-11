package br.com.desafio.tecnico.gestao.academico.domain;

/**
 * Enum em vez de booleano (D022 em docs/DECISIONS.md) - mesmo padrão do status de
 * Matrícula (PRD §02), mais extensível se novos estados surgirem na Fase 3+.
 */
public enum StatusTurma {

	ABERTA, FECHADA

}
