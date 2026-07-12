package br.com.desafio.tecnico.gestao.academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.desafio.tecnico.gestao.academico.domain.Matricula;
import br.com.desafio.tecnico.gestao.academico.domain.StatusMatricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

	/**
	 * @EntityGraph evita N+1 ao montar MatriculaResponse (aluno/turma são LAZY e
	 * acessados fora da transação do service, no mapeamento do controller) - mesmo
	 * achado de code review já corrigido em TurmaRepository na Fase 2, aplicado aqui
	 * proativamente.
	 */
	@EntityGraph(attributePaths = { "aluno", "turma" })
	List<Matricula> findByAlunoId(Long alunoId);

	@EntityGraph(attributePaths = { "aluno", "turma" })
	List<Matricula> findByTurmaId(Long turmaId);

	@EntityGraph(attributePaths = { "aluno", "turma" })
	Optional<Matricula> findById(Long id);

	/**
	 * Projeção escalar (não carrega a entidade Matricula/Aluno inteira) usada por
	 * AlunoOwnershipResolver (ABAC, D030) - uma consulta direta, sem N+1, em vez de
	 * findById(...).map(Matricula::getAluno).map(Aluno::getKeycloakSubjectId) (que
	 * também funcionaria, já que métodos de repositório são transacionais por padrão,
	 * mas carregaria a entidade inteira só para ler um campo).
	 */
	@Query("SELECT m.aluno.keycloakSubjectId FROM Matricula m WHERE m.id = :matriculaId")
	Optional<String> findAlunoKeycloakSubjectIdByMatriculaId(@Param("matriculaId") Long matriculaId);

	/**
	 * Checagem de duplicidade em código (D026), além do índice único parcial no banco
	 * (V6__academico_criar_tabela_matricula.sql) - dupla garantia, mesmo padrão de
	 * D020/D023. Uma matrícula CANCELADA não conta como "já matriculado".
	 */
	boolean existsByAlunoIdAndTurmaIdAndStatusNot(Long alunoId, Long turmaId, StatusMatricula status);

}
