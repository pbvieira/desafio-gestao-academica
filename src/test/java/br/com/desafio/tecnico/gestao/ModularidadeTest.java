package br.com.desafio.tecnico.gestao;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * specs/006-matricula.md, seção 4.3: primeira vez que o projeto tem mais de um módulo
 * com interação real entre eles (academico -> notificacao, via eventos de domínio,
 * D029). ApplicationModules.verify() reforça em tempo de teste os limites entre
 * módulos definidos pela estrutura de pacotes (CLAUDE.md, "Arquitetura-alvo") - falha
 * se algum código referenciar um tipo interno (subpacote) de outro módulo em vez de
 * usar sua API pública (pacote raiz) ou eventos.
 */
class ModularidadeTest {

	@Test
	void modulosRespeitamOsLimitesDeEncapsulamento() {
		ApplicationModules.of(GestaoApplication.class).verify();
	}

}
