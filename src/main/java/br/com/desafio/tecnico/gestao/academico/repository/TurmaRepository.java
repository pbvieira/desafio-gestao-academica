package br.com.desafio.tecnico.gestao.academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/**
	 * Consumo de vaga (D024/D025 em docs/DECISIONS.md): UPDATE condicional atômico -
	 * checagem do limite e decremento no mesmo statement, sem janela entre ler e
	 * escrever. Retorna 0 linhas afetadas quando a versão está desatualizada (edição
	 * concorrente de outro campo) OU quando as vagas já se esgotaram - o service
	 * distingue os dois casos com uma re-consulta, só no caminho de falha.
	 */
	/*
	 * clearAutomatically/flushAutomatically: o UPDATE em bulk não passa pelo
	 * persistence context - sem clearAutomatically, um findById(turmaId) logo depois
	 * (ex: para diferenciar VAGAS_ESGOTADAS de CONFLITO_CONCORRENCIA) devolveria a
	 * mesma instância já em cache (stale), não o estado real pós-UPDATE.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Turma t SET t.vagasOcupadas = t.vagasOcupadas + 1, t.version = t.version + 1 "
			+ "WHERE t.id = :id AND t.version = :version AND t.vagasOcupadas < t.limiteVagas")
	int consumirVaga(@Param("id") Long id, @Param("version") long version);

	/**
	 * Liberação de vaga - não há disputa por "quem libera primeiro" (ao contrário de
	 * consumir), então o service pode fazer um retry transparente de uma tentativa se
	 * 0 linhas forem afetadas por conflito de versão (specs/006, seção 4.4). Mesmo
	 * motivo de clearAutomatically/flushAutomatically do método acima.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Turma t SET t.vagasOcupadas = t.vagasOcupadas - 1, t.version = t.version + 1 "
			+ "WHERE t.id = :id AND t.version = :version AND t.vagasOcupadas > 0")
	int liberarVaga(@Param("id") Long id, @Param("version") long version);

}
