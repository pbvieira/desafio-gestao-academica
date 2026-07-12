package br.com.desafio.tecnico.gestao.academico;

/**
 * specs/006-matricula.md, seção 4.3. Publicado via ApplicationEventPublisher (D029 em
 * docs/DECISIONS.md - Modulith interno nesta fase, RabbitMQ só na Fase 4). Fica no
 * pacote raiz de `academico` (não em `academico.event`) de propósito: é a convenção do
 * Spring Modulith para que um tipo seja automaticamente parte da API pública do módulo
 * (consumível por `notificacao` sem precisar de `@NamedInterface`) - tipos em
 * subpacotes (`academico.domain`, `.service` etc.) são internos por padrão.
 */
public record MatriculaCriada(Long matriculaId, Long alunoId, Long turmaId) {
}
