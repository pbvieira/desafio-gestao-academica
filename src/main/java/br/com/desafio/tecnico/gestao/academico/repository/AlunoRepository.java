package br.com.desafio.tecnico.gestao.academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.desafio.tecnico.gestao.academico.domain.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	List<Aluno> findByAtivoTrue();

	Optional<Aluno> findByIdAndAtivoTrue(Long id);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	/**
	 * Checagem de unicidade de keycloakSubjectId (D030 em docs/DECISIONS.md) - além da
	 * constraint UNIQUE do banco (V1__academico_criar_tabela_aluno.sql), mesmo padrão
	 * de dupla garantia já usado para email/código em toda a Fase 2.
	 */
	boolean existsByKeycloakSubjectId(String keycloakSubjectId);

	boolean existsByKeycloakSubjectIdAndIdNot(String keycloakSubjectId, Long id);

	/**
	 * Projeção escalar usada por AlunoOwnershipResolver (ABAC, D030) - mesmo motivo de
	 * findAlunoKeycloakSubjectIdByMatriculaId em MatriculaRepository.
	 */
	@Query("SELECT a.keycloakSubjectId FROM Aluno a WHERE a.id = :alunoId")
	Optional<String> findKeycloakSubjectIdById(@Param("alunoId") Long alunoId);

	/**
	 * D041 em docs/DECISIONS.md: lookup reverso (subject Keycloak -> Aluno) para o
	 * endpoint de autoleitura GET /api/alunos/me - sem isso, o frontend (Fase 5) não
	 * tem como o próprio ALUNO descobrir seu alunoId para se matricular.
	 */
	Optional<Aluno> findByKeycloakSubjectIdAndAtivoTrue(String keycloakSubjectId);

}
