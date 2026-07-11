package br.com.desafio.tecnico.gestao.academico.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.domain.Aluno;
import br.com.desafio.tecnico.gestao.academico.dto.AlunoRequest;
import br.com.desafio.tecnico.gestao.academico.repository.AlunoRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

@Service
public class AlunoService {

	private final AlunoRepository alunoRepository;

	public AlunoService(AlunoRepository alunoRepository) {
		this.alunoRepository = alunoRepository;
	}

	@Transactional
	public Aluno criar(AlunoRequest request) {
		String email = normalizarEmail(request.email());
		if (alunoRepository.existsByEmail(email)) {
			throw new ConflitoRegraNegocioException("Já existe um aluno com o email '" + email + "'.");
		}
		Aluno aluno = new Aluno();
		aluno.setNome(request.nome());
		aluno.setEmail(email);
		return alunoRepository.save(aluno);
	}

	@Transactional
	public Aluno editar(Long id, AlunoRequest request) {
		Aluno aluno = buscarAtivo(id);
		String email = normalizarEmail(request.email());
		if (alunoRepository.existsByEmailAndIdNot(email, id)) {
			throw new ConflitoRegraNegocioException("Já existe um aluno com o email '" + email + "'.");
		}
		aluno.setNome(request.nome());
		aluno.setEmail(email);
		return aluno;
	}

	/**
	 * Achado de code review: sem normalização, "Ana@Example.com" e "ana@example.com"
	 * driblavam a checagem de unicidade em existsByEmail (case-sensitive) e a
	 * constraint UNIQUE do banco (colação case-sensitive) - duas contas para o mesmo
	 * email na prática.
	 */
	private String normalizarEmail(String email) {
		return email.trim().toLowerCase();
	}

	@Transactional(readOnly = true)
	public List<Aluno> listar() {
		return alunoRepository.findByAtivoTrue();
	}

	@Transactional(readOnly = true)
	public Aluno buscarAtivo(Long id) {
		return alunoRepository.findByIdAndAtivoTrue(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Aluno com id '" + id + "' não encontrado."));
	}

	@Transactional
	public void excluir(Long id) {
		Aluno aluno = buscarAtivo(id);
		aluno.inativar();
	}

}
