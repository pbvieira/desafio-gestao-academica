package br.com.desafio.tecnico.gestao.academico.concorrencia;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.desafio.tecnico.gestao.academico.domain.Turma;

/**
 * specs/012-concorrencia-e-testes.md, D051: existe só para comparar números com a
 * estratégia atômica já em produção (D024) - nunca referenciado por código de
 * src/main/java. Repository de teste dedicado, descoberto automaticamente pelo
 * component-scan do Spring Boot (br.com.desafio.tecnico.gestao.* sem restrição), sem
 * nenhuma configuração adicional.
 */
public interface TurmaLockPessimistaRepository extends JpaRepository<Turma, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Turma t where t.id = :id")
	Optional<Turma> buscarComLockPessimista(@Param("id") Long id);

}
