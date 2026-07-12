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
		String keycloakSubjectId = normalizarKeycloakSubjectId(request.keycloakSubjectId());
		validarKeycloakSubjectIdDisponivel(keycloakSubjectId, null);
		Aluno aluno = new Aluno();
		aluno.setNome(request.nome());
		aluno.setEmail(email);
		aluno.setKeycloakSubjectId(keycloakSubjectId);
		return alunoRepository.save(aluno);
	}

	@Transactional
	public Aluno editar(Long id, AlunoRequest request) {
		Aluno aluno = buscarAtivo(id);
		String email = normalizarEmail(request.email());
		if (alunoRepository.existsByEmailAndIdNot(email, id)) {
			throw new ConflitoRegraNegocioException("Já existe um aluno com o email '" + email + "'.");
		}
		String keycloakSubjectId = normalizarKeycloakSubjectId(request.keycloakSubjectId());
		validarKeycloakSubjectIdDisponivel(keycloakSubjectId, id);
		aluno.setNome(request.nome());
		aluno.setEmail(email);
		aluno.setKeycloakSubjectId(keycloakSubjectId);
		return aluno;
	}

	/**
	 * Achado de security review (D031 em docs/DECISIONS.md): sem isso, "" ou "  "
	 * eram aceitos como um valor "real" de keycloakSubjectId (inconsistente com a
	 * normalização já aplicada a email) - trata string em branco como "sem vínculo"
	 * (null), igual a não informar o campo.
	 */
	private String normalizarKeycloakSubjectId(String keycloakSubjectId) {
		if (keycloakSubjectId == null || keycloakSubjectId.isBlank()) {
			return null;
		}
		return keycloakSubjectId.trim();
	}

	/**
	 * D030 em docs/DECISIONS.md: keycloakSubjectId é opcional (null é sempre
	 * "disponível" - vários alunos podem estar sem vínculo), mas quando informado
	 * precisa ser único (reforçado pela constraint UNIQUE do banco também).
	 */
	private void validarKeycloakSubjectIdDisponivel(String keycloakSubjectId, Long idAtual) {
		if (keycloakSubjectId == null) {
			return;
		}
		boolean emUso = idAtual == null ? alunoRepository.existsByKeycloakSubjectId(keycloakSubjectId)
				: alunoRepository.existsByKeycloakSubjectIdAndIdNot(keycloakSubjectId, idAtual);
		if (emUso) {
			throw new ConflitoRegraNegocioException(
					"O keycloakSubjectId '" + keycloakSubjectId + "' já está vinculado a outro aluno.",
					"KEYCLOAK_SUBJECT_JA_VINCULADO");
		}
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
