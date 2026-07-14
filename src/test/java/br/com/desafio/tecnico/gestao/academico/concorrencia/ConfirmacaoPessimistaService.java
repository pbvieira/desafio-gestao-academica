package br.com.desafio.tecnico.gestao.academico.concorrencia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.domain.Turma;

/**
 * specs/012-concorrencia-e-testes.md, D051: réplica mínima só do ponto de acesso à vaga
 * que TurmaRepository.consumirVaga (D024, UPDATE condicional atômico) substitui em
 * produção - lê a Turma sob lock pessimista, checa o limite em Java, incrementa. Não
 * duplica MatriculaService inteiro (sem status de Matrícula, sem eventos) - existe só
 * para medir o ponto de contenção isoladamente.
 *
 * Achado da revisão final de branch (2026-07-13): por estar em src/test/java sob o pacote
 * base br.com.desafio.tecnico.gestao.*, este bean é escaneado e instanciado em TODO
 * @SpringBootTest do projeto (não só no teste de comparação), já que Java/Maven não
 * distinguem source roots no classpath. Isso é deliberado (D051) e inofensivo - depende só
 * de Turma/TurmaLockPessimistaRepository, sem nenhuma aresta nova entre módulos Modulith -
 * mas é um fato a saber, não a redescobrir se um teste não relacionado começar a falhar por
 * um problema de wiring aqui.
 */
@Service
public class ConfirmacaoPessimistaService {

	private final TurmaLockPessimistaRepository turmaLockPessimistaRepository;

	public ConfirmacaoPessimistaService(TurmaLockPessimistaRepository turmaLockPessimistaRepository) {
		this.turmaLockPessimistaRepository = turmaLockPessimistaRepository;
	}

	/**
	 * @return true se a vaga foi consumida, false se o limite já havia sido atingido.
	 */
	@Transactional
	public boolean confirmarComLockPessimista(Long turmaId) {
		Turma turma = turmaLockPessimistaRepository.buscarComLockPessimista(turmaId)
				.orElseThrow(() -> new IllegalStateException("Turma '" + turmaId + "' não encontrada."));
		if (turma.getVagasOcupadas() >= turma.getLimiteVagas()) {
			return false;
		}
		turma.setVagasOcupadas(turma.getVagasOcupadas() + 1);
		return true;
	}

}
