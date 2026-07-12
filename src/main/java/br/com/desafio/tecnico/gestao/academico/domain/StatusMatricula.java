package br.com.desafio.tecnico.gestao.academico.domain;

/**
 * Máquina de estados completa em D028 (docs/DECISIONS.md): PENDENTE->CONFIRMADA,
 * PENDENTE->CANCELADA, CONFIRMADA->CANCELADA, CONFIRMADA->CONFIRMADA (idempotente,
 * tratado no service, não aqui). CANCELADA é terminal.
 */
public enum StatusMatricula {

	PENDENTE, CONFIRMADA, CANCELADA

}
