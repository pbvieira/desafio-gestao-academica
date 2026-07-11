package br.com.desafio.tecnico.gestao.security.web;

import java.util.List;

/**
 * Claims do próprio JWT devolvidas como "perfil" - sem persistência, pois não há
 * entidade de usuário/perfil nesta fase (specs/003, seção 2).
 */
public record PerfilResponse(String subject, String username, String email, List<String> roles) {
}
