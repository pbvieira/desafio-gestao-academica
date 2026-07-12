package br.com.desafio.tecnico.gestao.academico.dto;

import java.time.Instant;

import br.com.desafio.tecnico.gestao.academico.domain.Aluno;

public record AlunoResponse(Long id, String nome, String email, String keycloakSubjectId, boolean ativo,
		Instant criadoEm) {

	public static AlunoResponse de(Aluno aluno) {
		return new AlunoResponse(aluno.getId(), aluno.getNome(), aluno.getEmail(), aluno.getKeycloakSubjectId(),
				aluno.isAtivo(), aluno.getCriadoEm());
	}

}
