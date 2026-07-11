package br.com.desafio.tecnico.gestao.academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.desafio.tecnico.gestao.academico.domain.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

	/**
	 * @EntityGraph evita N+1 ao montar TurmaResponse (curso/disciplina são LAZY e
	 * acessados fora da transação do service, no mapeamento do controller) - achado
	 * de code review.
	 */
	@EntityGraph(attributePaths = { "curso", "disciplina" })
	List<Turma> findByAtivoTrue();

	@EntityGraph(attributePaths = { "curso", "disciplina" })
	Optional<Turma> findByIdAndAtivoTrue(Long id);

	boolean existsByCursoIdAndDisciplinaIdAndCodigo(Long cursoId, Long disciplinaId, String codigo);

	boolean existsByCursoIdAndDisciplinaIdAndCodigoAndIdNot(Long cursoId, Long disciplinaId, String codigo, Long id);

	/**
	 * Usado pelo DisciplinaService para bloquear inativação de Disciplina com Turma
	 * ativa vinculada (D022) - Turma não é navegável a partir de Disciplina no
	 * mapeamento JPA, então a checagem passa pelo repositório.
	 */
	boolean existsByDisciplinaIdAndAtivoTrue(Long disciplinaId);

	/**
	 * Usado pelo CursoService para bloquear a desvinculação de uma Disciplina que
	 * ainda tem Turma referenciando o par (curso, disciplina) - achado de code
	 * review: sem esta checagem, a remoção da linha em curso_disciplina violava a FK
	 * composta de turma (V5) e vazava um 500 genérico em vez de um 409 de negócio.
	 * Não filtra por ativo=true de propósito: mesmo uma Turma inativada (soft
	 * delete) continua com a linha física em turma, então ainda bloqueia a FK.
	 */
	boolean existsByCursoIdAndDisciplinaId(Long cursoId, Long disciplinaId);

}
