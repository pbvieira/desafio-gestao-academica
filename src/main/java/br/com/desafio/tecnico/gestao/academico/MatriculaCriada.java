package br.com.desafio.tecnico.gestao.academico;

import java.util.UUID;

import org.springframework.modulith.events.Externalized;

import br.com.desafio.tecnico.gestao.config.MensageriaConfig;

/**
 * specs/006-matricula.md, seção 4.3 (publicação interna via ApplicationEventPublisher) +
 * specs/007-mensageria-rabbitmq.md, seção 4.2 (externalização para RabbitMQ, D032/D035).
 * Fica no pacote raiz de `academico` (não em `academico.event`) de propósito: é a
 * convenção do Spring Modulith para que um tipo seja automaticamente parte da API
 * pública do módulo (consumível por `notificacao` sem precisar de `@NamedInterface`) -
 * tipos em subpacotes (`academico.domain`, `.service` etc.) são internos por padrão.
 * `eventId` (D035): identificador estável por instância de evento, necessário para o
 * dedupe de idempotência no consumidor (viaja no payload JSON serializado).
 * `traceId` (D034): capturado sincronamente na thread da requisição HTTP original (ver
 * MatriculaService), não no momento da publicação para o RabbitMQ - a externalização do
 * Modulith roda em thread assíncrona própria (ex: "task-1"), onde o contexto de span do
 * Micrometer (baseado em ThreadLocal) não está presente; viaja no próprio payload do
 * evento pelo mesmo motivo que não dá para confiar num `MessagePostProcessor` no
 * `RabbitTemplate` para capturá-lo (achado empírico, não óbvio a priori).
 */
@Externalized(MensageriaConfig.EXCHANGE_EVENTOS + "::" + MensageriaConfig.ROUTING_KEY_MATRICULA_CRIADA)
public record MatriculaCriada(UUID eventId, String traceId, Long matriculaId, Long alunoId, Long turmaId) {
}
