package br.com.desafio.tecnico.gestao.academico;

/**
 * specs/006-matricula.md, seção 4.3. Publicado via ApplicationEventPublisher (D029 em
 * docs/DECISIONS.md) - não republicado na confirmação idempotente (CONFIRMADA->CONFIRMADA,
 * D028), só na transição real PENDENTE->CONFIRMADA.
 */
public record MatriculaConfirmada(Long matriculaId) {
}
