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
 * specs/005-dominio-base.md. limiteVagas e status já existem para a Fase 3 (Matrícula)
 * construir a lógica de consumo/liberação de vaga em cima - nenhuma lógica de vaga é
 * implementada aqui (fora de escopo desta fase). curso_id/disciplina_id são validados
 * (par precisa existir em curso_disciplina) no service E reforçados por FK composta no
 * banco (V5__academico_criar_tabela_turma.sql) - dupla garantia, D020.
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
