package br.com.desafio.tecnico.gestao.notificacao.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.desafio.tecnico.gestao.notificacao.domain.EventoProcessado;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, UUID> {
}
