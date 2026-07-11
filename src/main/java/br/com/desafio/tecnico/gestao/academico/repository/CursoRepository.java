package br.com.desafio.tecnico.gestao.academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.desafio.tecnico.gestao.academico.domain.Curso;

public interface CursoRepository extends JpaRepository<Curso, Long> {

	List<Curso> findByAtivoTrue();

	Optional<Curso> findByIdAndAtivoTrue(Long id);

	boolean existsByCodigo(String codigo);

	boolean existsByCodigoAndIdNot(String codigo, Long id);

}
