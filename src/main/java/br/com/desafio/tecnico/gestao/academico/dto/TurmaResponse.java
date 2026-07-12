package br.com.desafio.tecnico.gestao.academico.dto;

import java.time.Instant;

import br.com.desafio.tecnico.gestao.academico.domain.StatusTurma;
import br.com.desafio.tecnico.gestao.academico.domain.Turma;

public record TurmaResponse(Long id, String codigo, Long cursoId, String cursoNome, Long disciplinaId,
		String disciplinaNome, int limiteVagas, int vagasOcupadas, StatusTurma status, boolean ativo,
		Instant criadoEm) {

	public static TurmaResponse de(Turma turma) {
		return new TurmaResponse(turma.getId(), turma.getCodigo(), turma.getCurso().getId(),
				turma.getCurso().getNome(), turma.getDisciplina().getId(), turma.getDisciplina().getNome(),
				turma.getLimiteVagas(), turma.getVagasOcupadas(), turma.getStatus(), turma.isAtivo(),
				turma.getCriadoEm());
	}

}
