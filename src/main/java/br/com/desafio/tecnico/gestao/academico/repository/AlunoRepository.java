package br.com.desafio.tecnico.gestao.academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.desafio.tecnico.gestao.academico.domain.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	List<Aluno> findByAtivoTrue();

	Optional<Aluno> findByIdAndAtivoTrue(Long id);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

}
