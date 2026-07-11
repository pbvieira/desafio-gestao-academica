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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * specs/005-dominio-base.md. N:N com Disciplina (D020) - uma Disciplina pode ser
 * oferecida em mais de um Curso.
 */
@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class Curso {

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

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "curso_disciplina", joinColumns = @JoinColumn(name = "curso_id"),
			inverseJoinColumns = @JoinColumn(name = "disciplina_id"))
	private Set<Disciplina> disciplinas = new HashSet<>();

	public void inativar() {
		this.ativo = false;
	}

	/**
	 * Regra implícita (D022): não é possível inativar um Curso com Disciplina ativa
	 * vinculada - o service consulta isto antes de chamar inativar().
	 */
	public boolean temDisciplinaAtivaVinculada() {
		return disciplinas.stream().anyMatch(Disciplina::isAtivo);
	}

}
