package br.com.desafio.tecnico.gestao.academico.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * specs/005-dominio-base.md. keycloakSubjectId executa D006 (mapeamento Keycloak<->Aluno
 * adiado até Aluno existir) - nenhum código consome esse campo ainda nesta fase.
 */
@Entity
@Table(name = "aluno")
@Getter
@Setter
@NoArgsConstructor
public class Aluno {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(nullable = false, length = 200, unique = true)
	private String email;

	@Column(name = "keycloak_subject_id", length = 64, unique = true)
	private String keycloakSubjectId;

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm = Instant.now();

	public void inativar() {
		this.ativo = false;
	}

}
