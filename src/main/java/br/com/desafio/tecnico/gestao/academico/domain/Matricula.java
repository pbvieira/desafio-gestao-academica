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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * specs/006-matricula.md. Sem soft delete (diferente do padrão D019 de
 * Aluno/Curso/Disciplina/Turma) - matrícula cancelada é um estado (StatusMatricula),
 * não uma exclusão; o registro histórico é o próprio propósito da entidade.
 */
@Entity
@Table(name = "matricula")
@Getter
@Setter
@NoArgsConstructor
public class Matricula {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "aluno_id", nullable = false)
	private Aluno aluno;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "turma_id", nullable = false)
	private Turma turma;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusMatricula status = StatusMatricula.PENDENTE;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm = Instant.now();

	@Column(name = "confirmado_em")
	private Instant confirmadoEm;

	@Column(name = "cancelado_em")
	private Instant canceladoEm;

}
