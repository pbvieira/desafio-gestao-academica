package br.com.desafio.tecnico.gestao.academico.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * specs/005-dominio-base.md (limiteVagas/status) + specs/006-matricula.md
 * (vagasOcupadas/version). curso_id/disciplina_id são validados (par precisa existir em
 * curso_disciplina) no service E reforçados por FK composta no banco
 * (V5__academico_criar_tabela_turma.sql) - dupla garantia, D020. vagasOcupadas é
 * atualizado só via UPDATE condicional atômico no TurmaRepository (D024/D025) - nunca
 * via setVagasOcupadas()+save() direto, que perderia a checagem atômica do limite.
 * version é o lock otimista do JPA (@Version) para os demais campos da Turma.
 */
@Entity
@Table(name = "turma")
@Getter
@Setter
@NoArgsConstructor
public class Turma {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String codigo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "curso_id", nullable = false)
	private Curso curso;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disciplina_id", nullable = false)
	private Disciplina disciplina;

	@Column(name = "limite_vagas", nullable = false)
	private int limiteVagas;

	@Column(name = "vagas_ocupadas", nullable = false)
	private int vagasOcupadas;

	@Version
	@Column(nullable = false)
	private long version;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusTurma status = StatusTurma.ABERTA;

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm = Instant.now();

	public void inativar() {
		this.ativo = false;
	}

}
