package br.com.desafio.tecnico.gestao.academico;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.desafio.tecnico.gestao.academico.repository.AlunoRepository;
import br.com.desafio.tecnico.gestao.academico.repository.MatriculaRepository;

/**
 * API pública mínima do módulo `academico` para resolução de posse (ABAC, D030 em
 * docs/DECISIONS.md), consumida pelo `PerfilPermissionEvaluator` do módulo `security`.
 * Fica no pacote raiz de propósito (mesma razão dos eventos, ver MatriculaCriada) - o
 * módulo `security` não pode enxergar `academico.repository` (interno), mas pode
 * enxergar qualquer tipo no pacote raiz de `academico`. Isso evita que `security`
 * precise conhecer JPA/entidades de `academico`, só "quem é o dono deste id".
 */
@Component
public class AlunoOwnershipResolver {

	private final AlunoRepository alunoRepository;
	private final MatriculaRepository matriculaRepository;

	public AlunoOwnershipResolver(AlunoRepository alunoRepository, MatriculaRepository matriculaRepository) {
		this.alunoRepository = alunoRepository;
		this.matriculaRepository = matriculaRepository;
	}

	public Optional<String> keycloakSubjectIdDoAluno(Long alunoId) {
		return alunoRepository.findKeycloakSubjectIdById(alunoId);
	}

	public Optional<String> keycloakSubjectIdDoAlunoDaMatricula(Long matriculaId) {
		return matriculaRepository.findAlunoKeycloakSubjectIdByMatriculaId(matriculaId);
	}

}
