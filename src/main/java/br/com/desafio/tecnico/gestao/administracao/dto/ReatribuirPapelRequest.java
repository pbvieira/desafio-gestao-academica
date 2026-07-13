package br.com.desafio.tecnico.gestao.administracao.dto;

import jakarta.validation.constraints.NotNull;

public record ReatribuirPapelRequest(@NotNull Papel papel) {
}
