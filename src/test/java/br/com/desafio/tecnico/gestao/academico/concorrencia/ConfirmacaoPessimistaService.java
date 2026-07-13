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
