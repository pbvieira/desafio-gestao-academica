package br.com.desafio.tecnico.gestao.academico;

/**
 * specs/006-matricula.md, seção 4.3. Publicado via ApplicationEventPublisher (D029 em
 * docs/DECISIONS.md). liberouVaga distingue cancelamento de PENDENTE (nunca ocupou vaga)
 * de cancelamento de CONFIRMADA (liberou vaga) - útil para um consumidor futuro decidir
 * se precisa notificar sobre vaga disponível.
 */
public record MatriculaCancelada(Long matriculaId, boolean liberouVaga) {
}
