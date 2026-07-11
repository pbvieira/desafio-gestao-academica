package br.com.desafio.tecnico.gestao.academico.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * specs/005-dominio-base.md. Item de catálogo compartilhável entre Cursos (D020) - por
 * isso o código é único globalmente, não por curso.
 */
@Entity
@Table(name = "disciplina")
@Getter
@Setter
@NoArgsConstructor
public class Disciplina {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20, unique = true)
	private String codigo;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm = Instant.now();

	@ManyToMany(mappedBy = "disciplinas", fetch = FetchType.LAZY)
	private Set<Curso> cursos = new HashSet<>();

	public void inativar() {
		this.ativo = false;
	}

	/**
	 * Regra implícita (D022): não é possível inativar uma Disciplina com Turma ativa
	 * vinculada - o service consulta isto (via TurmaRepository) antes de chamar
	 * inativar(), não por este método (Turma não é navegável a partir de Disciplina
	 * no mapeamento JPA, para não acoplar os dois lados desnecessariamente).
	 */

}
