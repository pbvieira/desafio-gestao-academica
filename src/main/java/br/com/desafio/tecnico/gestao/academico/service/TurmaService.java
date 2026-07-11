package br.com.desafio.tecnico.gestao.academico.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.tecnico.gestao.academico.domain.Curso;
import br.com.desafio.tecnico.gestao.academico.domain.Disciplina;
import br.com.desafio.tecnico.gestao.academico.domain.Turma;
import br.com.desafio.tecnico.gestao.academico.dto.TurmaRequest;
import br.com.desafio.tecnico.gestao.academico.repository.CursoRepository;
import br.com.desafio.tecnico.gestao.academico.repository.DisciplinaRepository;
import br.com.desafio.tecnico.gestao.academico.repository.TurmaRepository;
import br.com.desafio.tecnico.gestao.errorhandling.ConflitoRegraNegocioException;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

/**
 * Nenhuma lógica de vaga/concorrência aqui (fora de escopo desta fase, specs/005,
 * seção 2) - limiteVagas e status só existem como dado, prontos para a Fase 3.
 */
@Service
public class TurmaService {

	private final TurmaRepository turmaRepository;
	private final CursoRepository cursoRepository;
	private final DisciplinaRepository disciplinaRepository;

	public TurmaService(TurmaRepository turmaRepository, CursoRepository cursoRepository,
			DisciplinaRepository disciplinaRepository) {
		this.turmaRepository = turmaRepository;
		this.cursoRepository = cursoRepository;
		this.disciplinaRepository = disciplinaRepository;
	}

	@Transactional
	public Turma criar(TurmaRequest request) {
		Curso curso = buscarCursoAtivo(request.cursoId());
		Disciplina disciplina = buscarDisciplinaAtiva(request.disciplinaId());
		validarParCursoDisciplina(curso, disciplina);
		String codigo = normalizarCodigo(request.codigo());
		if (turmaRepository.existsByCursoIdAndDisciplinaIdAndCodigo(curso.getId(), disciplina.getId(), codigo)) {
			throw new ConflitoRegraNegocioException(
					"Já existe uma turma com o código '" + codigo + "' para esta disciplina neste curso.");
		}
		Turma turma = new Turma();
		turma.setCodigo(codigo);
		turma.setCurso(curso);
		turma.setDisciplina(disciplina);
		turma.setLimiteVagas(request.limiteVagas());
		return turmaRepository.save(turma);
	}

	@Transactional
	public Turma editar(Long id, TurmaRequest request) {
		Turma turma = buscarAtiva(id);
		Curso curso = buscarCursoAtivo(request.cursoId());
		Disciplina disciplina = buscarDisciplinaAtiva(request.disciplinaId());
		validarParCursoDisciplina(curso, disciplina);
		String codigo = normalizarCodigo(request.codigo());
		if (turmaRepository.existsByCursoIdAndDisciplinaIdAndCodigoAndIdNot(curso.getId(), disciplina.getId(),
				codigo, id)) {
			throw new ConflitoRegraNegocioException(
					"Já existe uma turma com o código '" + codigo + "' para esta disciplina neste curso.");
		}
		turma.setCodigo(codigo);
		turma.setCurso(curso);
		turma.setDisciplina(disciplina);
		turma.setLimiteVagas(request.limiteVagas());
		return turma;
	}

	/**
	 * Achado de code review (mesma classe de problema em Aluno/Curso/Disciplina):
	 * sem normalização, "t1" e "T1" driblavam a checagem de unicidade
	 * (curso, disciplina, código).
	 */
	private String normalizarCodigo(String codigo) {
		return codigo.trim().toUpperCase();
	}

	@Transactional(readOnly = true)
	public List<Turma> listar() {
		return turmaRepository.findByAtivoTrue();
	}

	@Transactional(readOnly = true)
	public Turma buscarAtiva(Long id) {
		return turmaRepository.findByIdAndAtivoTrue(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Turma com id '" + id + "' não encontrada."));
	}

	@Transactional
	public void excluir(Long id) {
		Turma turma = buscarAtiva(id);
		turma.inativar();
	}

	private Curso buscarCursoAtivo(Long cursoId) {
		return cursoRepository.findByIdAndAtivoTrue(cursoId).orElseThrow(
				() -> new RecursoNaoEncontradoException("Curso com id '" + cursoId + "' não encontrado."));
	}

	private Disciplina buscarDisciplinaAtiva(Long disciplinaId) {
		return disciplinaRepository.findByIdAndAtivoTrue(disciplinaId).orElseThrow(
				() -> new RecursoNaoEncontradoException("Disciplina com id '" + disciplinaId + "' não encontrada."));
	}

	/**
	 * D020: uma Turma só pode existir para um par (curso, disciplina) já associado
	 * via curso_disciplina - reforçado aqui e por FK composta no banco (dupla
	 * garantia).
	 */
	private void validarParCursoDisciplina(Curso curso, Disciplina disciplina) {
		boolean associados = curso.getDisciplinas().stream()
				.anyMatch(d -> d.getId().equals(disciplina.getId()));
		if (!associados) {
			throw new ConflitoRegraNegocioException("A disciplina '" + disciplina.getCodigo()
					+ "' não está associada ao curso '" + curso.getCodigo() + "'.");
		}
	}

}
