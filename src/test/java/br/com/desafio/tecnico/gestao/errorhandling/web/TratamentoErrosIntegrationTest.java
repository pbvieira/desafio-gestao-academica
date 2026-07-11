package br.com.desafio.tecnico.gestao.errorhandling.web;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um caso por status de erro (specs/004, seção 7). 401/403 do fluxo de segurança já são
 * cobertos em AutorizacaoIntegrationTest (specs/003); aqui o foco é 400/404/409/500 e a
 * consistência do formato ProblemDetail (type/errorCode) entre eles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TratamentoErrosIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	// Usado só no teste do 500 (spy sobre o bean real) - força uma exceção inesperada
	// através do dispatch real do MVC, sem precisar de um endpoint "quebra de
	// propósito" em código de produção (pedido explícito da spec 004, seção 7).
	@MockitoSpyBean
	private ExemploRecursoController exemploRecursoController;

	@Test
	void recursoNaoEncontradoRetorna404ComProblemDetail() throws Exception {
		mockMvc.perform(get("/api/exemplo/recurso/inexistente").with(jwt()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.type").value("urn:gestao:erro:resource-not-found"));
	}

	@Test
	void validacaoInvalidaRetorna400ComListaDeErros() throws Exception {
		mockMvc.perform(post("/api/exemplo/recurso").with(jwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.erros").isArray());
	}

	@Test
	void corpoMalformadoRetorna400ComProblemDetail() throws Exception {
		mockMvc.perform(post("/api/exemplo/recurso").with(jwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": nao-e-json-valido"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void conflitoDeRegraRetorna409ComProblemDetail() throws Exception {
		mockMvc.perform(post("/api/exemplo/recurso").with(jwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"existe\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("BUSINESS_CONFLICT"));
	}

	@Test
	void recursoExistenteRetorna200() throws Exception {
		mockMvc.perform(get("/api/exemplo/recurso/existe").with(jwt()))
				.andExpect(status().isOk());
	}

	@Test
	void preAuthorizeNegadoEmControllerRetorna403ComProblemDetail() throws Exception {
		mockMvc.perform(get("/api/exemplo/staff").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUNO"))))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
				.andExpect(jsonPath("$.type").value("urn:gestao:erro:access-denied"));
	}

	@Test
	void erroInesperadoNaoTratadoRetorna500ComMensagemGenerica() throws Exception {
		Mockito.doThrow(new IllegalStateException("detalhe interno sensivel: senha=1234"))
				.when(exemploRecursoController)
				.buscar("existe");

		mockMvc.perform(get("/api/exemplo/recurso/existe").with(jwt()))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.detail").value("Erro interno. Contate o suporte."))
				.andExpect(jsonPath("$.detail", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("senha"))));
	}

}
