package br.com.desafio.tecnico.gestao;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

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

	/**
	 * specs/013-finalizacao.md, D056: gera a saída PlantUML do Documenter do Modulith -
	 * evidência automática de que a modularização declarada em docs/ARQUITETURA.md é a
	 * real, derivada do mesmo ApplicationModules usado por verify() acima. Não é um
	 * teste de asserção (sem @Test de verificação) - roda uma vez, manualmente, para
	 * gerar o artefato comitado em docs/architecture/. Não faz parte do gate de CI
	 * (não precisa rodar a cada build; o diagrama só muda se a estrutura de módulos
	 * mudar).
	 */
	@Test
	void geraDocumentacaoDeModulos() {
		new Documenter(ApplicationModules.of(GestaoApplication.class)).writeModulesAsPlantUml();
	}

}
