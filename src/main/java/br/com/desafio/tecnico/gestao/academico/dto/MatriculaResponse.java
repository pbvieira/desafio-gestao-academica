package br.com.desafio.tecnico.gestao.academico.dto;

import java.time.Instant;

import br.com.desafio.tecnico.gestao.academico.domain.Matricula;
import br.com.desafio.tecnico.gestao.academico.domain.StatusMatricula;

public record MatriculaResponse(Long id, Long alunoId, String alunoNome, Long turmaId, String turmaCodigo,
		StatusMatricula status, Instant criadoEm, Instant confirmadoEm, Instant canceladoEm) {

	public static MatriculaResponse de(Matricula matricula) {
		return new MatriculaResponse(matricula.getId(), matricula.getAluno().getId(),
				matricula.getAluno().getNome(), matricula.getTurma().getId(), matricula.getTurma().getCodigo(),
				matricula.getStatus(), matricula.getCriadoEm(), matricula.getConfirmadoEm(),
				matricula.getCanceladoEm());
	}

}
