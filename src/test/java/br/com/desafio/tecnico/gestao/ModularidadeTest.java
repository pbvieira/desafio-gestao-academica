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
	 * real, derivada do mesmo ApplicationModules usado por verify() acima. Roda a cada
	 * build (é um @Test comum, faz parte do gate de CI como qualquer outro), mas não
	 * faz nenhuma asserção - só regrava o artefato em docs/architecture/, que só muda
	 * de fato se a estrutura de módulos mudar.
	 */
	@Test
	void geraDocumentacaoDeModulos() {
		new Documenter(ApplicationModules.of(GestaoApplication.class)).writeModulesAsPlantUml();
	}

}
