package br.com.desafio.tecnico.gestao.academico.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.dto.DisciplinaRequest;
import br.com.desafio.tecnico.gestao.academico.repository.DisciplinaRepository;
import br.com.desafio.tecnico.gestao.academico.repository.TurmaRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

@Service
public class DisciplinaService {

	private final DisciplinaRepository disciplinaRepository;
	private final TurmaRepository turmaRepository;

	public DisciplinaService(DisciplinaRepository disciplinaRepository, TurmaRepository turmaRepository) {
		this.disciplinaRepository = disciplinaRepository;
		this.turmaRepository = turmaRepository;
	}

	@Transactional
	public Disciplina criar(DisciplinaRequest request) {
		String codigo = normalizarCodigo(request.codigo());
		if (disciplinaRepository.existsByCodigo(codigo)) {
			throw new ConflitoRegraNegocioException("Já existe uma disciplina com o código '" + codigo + "'.");
		}
		Disciplina disciplina = new Disciplina();
		disciplina.setCodigo(codigo);
		disciplina.setNome(request.nome());
		return disciplinaRepository.save(disciplina);
	}

	@Transactional
	public Disciplina editar(Long id, DisciplinaRequest request) {
		Disciplina disciplina = buscarAtivo(id);
		String codigo = normalizarCodigo(request.codigo());
		if (disciplinaRepository.existsByCodigoAndIdNot(codigo, id)) {
			throw new ConflitoRegraNegocioException("Já existe uma disciplina com o código '" + codigo + "'.");
		}
		disciplina.setCodigo(codigo);
		disciplina.setNome(request.nome());
		return disciplina;
	}

	/**
	 * Achado de code review: sem normalização, "mat101" e "MAT101" driblavam a
	 * checagem de unicidade em existsByCodigo (case-sensitive) e a constraint UNIQUE
	 * do banco.
	 */
	private String normalizarCodigo(String codigo) {
		return codigo.trim().toUpperCase();
	}

	@Transactional(readOnly = true)
	public List<Disciplina> listar() {
		return disciplinaRepository.findByAtivoTrue();
	}

	@Transactional(readOnly = true)
	public Disciplina buscarAtivo(Long id) {
		return disciplinaRepository.findByIdAndAtivoTrue(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("Disciplina com id '" + id + "' não encontrada."));
	}

	@Transactional
	public void excluir(Long id) {
		Disciplina disciplina = buscarAtivo(id);
		if (turmaRepository.existsByDisciplinaIdAndAtivoTrue(id)) {
			throw new ConflitoRegraNegocioException("Não é possível excluir a disciplina '" + disciplina.getCodigo()
					+ "': há turmas ativas vinculadas.");
		}
		disciplina.inativar();
	}

}
