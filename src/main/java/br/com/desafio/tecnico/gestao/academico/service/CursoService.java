package br.com.desafio.tecnico.gestao.academico.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.domain.Curso;
import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.dto.CursoRequest;
import br.com.desafio.tecnico.gestao.academico.repository.CursoRepository;
import br.com.desafio.tecnico.gestao.academico.repository.DisciplinaRepository;
import br.com.desafio.tecnico.gestao.academico.repository.TurmaRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

@Service
public class CursoService {

	private final CursoRepository cursoRepository;
	private final DisciplinaRepository disciplinaRepository;
	private final TurmaRepository turmaRepository;

	public CursoService(CursoRepository cursoRepository, DisciplinaRepository disciplinaRepository,
			TurmaRepository turmaRepository) {
		this.cursoRepository = cursoRepository;
		this.disciplinaRepository = disciplinaRepository;
		this.turmaRepository = turmaRepository;
	}

	@Transactional
	public Curso criar(CursoRequest request) {
		String codigo = normalizarCodigo(request.codigo());
		if (cursoRepository.existsByCodigo(codigo)) {
			throw new ConflitoRegraNegocioException("Já existe um curso com o código '" + codigo + "'.");
		}
		Curso curso = new Curso();
		curso.setCodigo(codigo);
		curso.setNome(request.nome());
		return cursoRepository.save(curso);
	}

	@Transactional
	public Curso editar(Long id, CursoRequest request) {
		Curso curso = buscarAtivo(id);
		String codigo = normalizarCodigo(request.codigo());
		if (cursoRepository.existsByCodigoAndIdNot(codigo, id)) {
			throw new ConflitoRegraNegocioException("Já existe um curso com o código '" + codigo + "'.");
		}
		curso.setCodigo(codigo);
		curso.setNome(request.nome());
		return curso;
	}

	/**
	 * Achado de code review: sem normalização, "ads" e "ADS" driblavam a checagem
	 * de unicidade em existsByCodigo (case-sensitive) e a constraint UNIQUE do banco.
	 */
	private String normalizarCodigo(String codigo) {
		return codigo.trim().toUpperCase();
	}

	@Transactional(readOnly = true)
	public List<Curso> listar() {
		return cursoRepository.findByAtivoTrue();
	}

	@Transactional(readOnly = true)
	public Curso buscarAtivo(Long id) {
		return cursoRepository.findByIdAndAtivoTrue(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Curso com id '" + id + "' não encontrado."));
	}

	@Transactional
	public void excluir(Long id) {
		Curso curso = buscarAtivo(id);
		if (curso.temDisciplinaAtivaVinculada()) {
			throw new ConflitoRegraNegocioException(
					"Não é possível excluir o curso '" + curso.getCodigo() + "': há disciplinas ativas vinculadas.");
		}
		curso.inativar();
	}

	/**
	 * Gerencia a associação N:N Curso<->Disciplina (D020) - sem isto, nenhuma Turma
	 * poderia ser criada (TurmaService exige o par já associado). Endpoint descoberto
	 * como necessário durante a implementação, não previsto na primeira versão da
	 * spec 005 - documentado na spec ao fechar a fase.
	 */
	@Transactional
	public Curso vincularDisciplina(Long cursoId, Long disciplinaId) {
		Curso curso = buscarAtivo(cursoId);
		Disciplina disciplina = disciplinaRepository.findByIdAndAtivoTrue(disciplinaId).orElseThrow(
				() -> new RecursoNaoEncontradoException("Disciplina com id '" + disciplinaId + "' não encontrada."));
		curso.getDisciplinas().add(disciplina);
		return curso;
	}

	@Transactional
	public Curso desvincularDisciplina(Long cursoId, Long disciplinaId) {
		Curso curso = buscarAtivo(cursoId);
		boolean associada = curso.getDisciplinas().stream().anyMatch(d -> d.getId().equals(disciplinaId));
		if (!associada) {
			throw new RecursoNaoEncontradoException(
					"Disciplina com id '" + disciplinaId + "' não está associada a este curso.");
		}
		/*
		 * Achado de code review: sem esta checagem, a remoção abaixo violava a FK
		 * composta de turma (V5__academico_criar_tabela_turma.sql) e vazava um 500
		 * genérico (GlobalExceptionHandler não trata DataIntegrityViolationException)
		 * em vez do 409 de negócio usado em toda a API.
		 */
		if (turmaRepository.existsByCursoIdAndDisciplinaId(cursoId, disciplinaId)) {
			throw new ConflitoRegraNegocioException("Não é possível desvincular a disciplina '" + disciplinaId
					+ "' do curso '" + cursoId + "': há turma(s) vinculadas a este par.");
		}
		curso.getDisciplinas().removeIf(d -> d.getId().equals(disciplinaId));
		return curso;
	}

}
