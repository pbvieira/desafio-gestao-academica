# Administração de Usuários e Papéis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar a um ADMIN a capacidade de listar usuários do Keycloak e reatribuir o papel
(ALUNO/SECRETARIA/ADMIN) de um usuário existente, via UI, sem abrir o console admin do Keycloak
(specs/010-administracao-usuarios-papeis.md).

**Architecture:** Novo módulo Modulith `administracao` (backend) que fala com a Admin API do Keycloak
via `keycloak-admin-client` (SDK oficial), usando o client confidential `gestao-backend` com grant
`client_credentials`. Endpoint ADMIN-only (`@PreAuthorize` de classe). Frontend: nova feature
`administracao/` reaproveitando os padrões já usados em `features/aluno/`, com a rota e o item de sidebar
que a spec 009 deixou reservados (D046).

**Tech Stack:** Spring Boot 3.5.16, Java 21, `org.keycloak:keycloak-admin-client:26.0.10`, Angular 20
standalone.

## Global Constraints

- Escopo: só reatribuir papel de usuário existente — sem criar usuário, sem resetar senha, sem
  habilitar/desabilitar (D045).
- Acesso: só ADMIN (`@PreAuthorize("hasRole('ADMIN')")` de classe inteira, sem excecão) — D045.
- Integração via `keycloak-admin-client` (SDK oficial), não REST manual — D045.
- Endpoints: `GET /api/admin/usuarios`, `PATCH /api/admin/usuarios/{id}/papel`.
- Nenhuma exceção nova: reusar `RecursoNaoEncontradoException` (404) já existente; "papel inválido" (400)
  via enum Java + `HttpMessageNotReadableException` já tratado pelo `GlobalExceptionHandler` — nenhum
  código de erro novo.
- Módulo novo `br.com.desafio.tecnico.gestao.administracao` (D046) — não colocar em `security`.
- Client `gestao-backend` precisa de `serviceAccountsEnabled: true` + client roles `view-users`/
  `manage-users`/`query-users` de `realm-management` (D046) — sem isso a Admin API nunca autentica.

---

## Mapa de arquivos

**Backend (criar):**
- `src/main/java/br/com/desafio/tecnico/gestao/administracao/config/KeycloakAdminConfig.java` — bean `Keycloak`
- `src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/Papel.java` — enum ALUNO/SECRETARIA/ADMIN
- `src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/UsuarioAdminDto.java`
- `src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/ReatribuirPapelRequest.java`
- `src/main/java/br/com/desafio/tecnico/gestao/administracao/service/AdministracaoUsuarioService.java`
- `src/main/java/br/com/desafio/tecnico/gestao/administracao/web/AdministracaoUsuarioController.java`
- `src/test/java/br/com/desafio/tecnico/gestao/administracao/service/AdministracaoUsuarioServiceTest.java`
- `src/test/java/br/com/desafio/tecnico/gestao/administracao/AdministracaoUsuarioControllerIntegrationTest.java`

**Backend (modificar):**
- `pom.xml` — nova dependência
- `docker/keycloak/import/gestao-realm.json` — client `gestao-backend`
- `src/main/resources/application.properties` — propriedades do admin client

**Frontend (criar):**
- `frontend/src/app/shared/models/usuario-admin.model.ts`
- `frontend/src/app/features/administracao/usuario-admin.service.ts`
- `frontend/src/app/features/administracao/usuario-admin.service.spec.ts`
- `frontend/src/app/features/administracao/usuarios-lista.component.ts`

**Frontend (modificar):**
- `frontend/src/app/app.routes.ts` — nova rota
- `frontend/src/app/app.html` — nova seção "Administração" na sidebar

**E2E (criar):**
- `e2e/administracao-papel-flow.sh`

---

### Task 1: Config do Keycloak — service account no `gestao-backend`

**Files:**
- Modify: `docker/keycloak/import/gestao-realm.json`

**Interfaces:**
- Consumes: nada.
- Produces: client `gestao-backend` autenticável via `client_credentials`, com permissão de ler/gerenciar
  usuários do realm `gestao` — consumido pela Task 2 (bean `Keycloak`).

- [ ] **Step 1: Editar o client `gestao-backend` no realm export**

Localize o bloco do client `gestao-backend` em `docker/keycloak/import/gestao-realm.json`:

```json
    {
      "clientId": "gestao-backend",
      "enabled": true,
      "publicClient": false,
      "secret": "${env.KEYCLOAK_BACKEND_CLIENT_SECRET}",
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": false
    }
```

Troque para:

```json
    {
      "clientId": "gestao-backend",
      "enabled": true,
      "publicClient": false,
      "secret": "${env.KEYCLOAK_BACKEND_CLIENT_SECRET}",
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": true,
      "serviceAccountClientRoles": {
        "realm-management": ["view-users", "manage-users", "query-users"]
      }
    }
```

- [ ] **Step 2: Recriar a stack local para forçar o reimport do realm**

O Postgres deste projeto não tem volume persistente (só o bind mount somente-leitura dos scripts de
init, `compose.yaml`) — `docker compose down` seguido de `up -d` já apaga todo o estado anterior
(inclusive o realm já importado do Keycloak) e força um reimport genuíno do JSON editado. Isso também
zera qualquer dado de domínio local (Aluno/Curso/Turma/Matrícula) — esperado e aceitável em
desenvolvimento local, não é uma perda de dado real.

Run (raiz do repo):
```bash
docker compose down
docker compose up -d
```

Expected: os 4 containers (postgres, redis, rabbitmq, keycloak) sobem limpos; `docker compose logs
keycloak | grep -i "Imported realm"` mostra o realm `gestao` sendo importado.

- [ ] **Step 3: Verificar que o service account autentica**

Run (raiz do repo, após aguardar ~10s o Keycloak ficar pronto):
```bash
source .env
curl -s -X POST "http://localhost:${KEYCLOAK_HTTP_PORT:-8081}/realms/gestao/protocol/openid-connect/token" \
  -d "client_id=gestao-backend" -d "client_secret=$KEYCLOAK_BACKEND_CLIENT_SECRET" \
  -d "grant_type=client_credentials"
```

Expected: JSON com um campo `"access_token": "..."` (sucesso). Se vier
`{"error":"unauthorized_client","error_description":"Client not enabled to retrieve service account"}`,
o Step 1/2 não foi aplicado corretamente — confira o JSON e repita o Step 2.

- [ ] **Step 4: Commit**

```bash
git add docker/keycloak/import/gestao-realm.json
git commit -m "feat: habilita service account do client gestao-backend (D045/D046)"
```

---

### Task 2: Dependência, config Spring e bean do cliente admin

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Create: `src/main/java/br/com/desafio/tecnico/gestao/administracao/config/KeycloakAdminConfig.java`

**Interfaces:**
- Consumes: propriedades `gestao.keycloak-admin.*` (novas, Step 2 abaixo).
- Produces: bean Spring `Keycloak` (tipo `org.keycloak.admin.client.Keycloak`) — consumido pela Task 3
  (`AdministracaoUsuarioService`).

- [ ] **Step 1: Adicionar a dependência em `pom.xml`**

Dentro do bloco `<dependencies>`, junto das demais (ex: após a dependência
`spring-boot-starter-oauth2-resource-server`):

```xml
		<dependency>
			<groupId>org.keycloak</groupId>
			<artifactId>keycloak-admin-client</artifactId>
			<version>26.0.10</version>
		</dependency>
```

- [ ] **Step 2: Adicionar propriedades em `src/main/resources/application.properties`**

Adicione ao final do arquivo:

```properties
# Cliente administrativo do Keycloak (specs/010) - usado só pelo módulo administracao para
# listar usuários e reatribuir papel via Admin API. client_credentials com o client
# confidential gestao-backend (D013), cujo service account ganhou os client roles de
# realm-management view-users/manage-users/query-users (D045/D046).
gestao.keycloak-admin.server-url=http://localhost:${KEYCLOAK_HTTP_PORT:8081}
gestao.keycloak-admin.realm=gestao
gestao.keycloak-admin.client-id=gestao-backend
gestao.keycloak-admin.client-secret=${KEYCLOAK_BACKEND_CLIENT_SECRET}
```

- [ ] **Step 3: Criar `KeycloakAdminConfig.java`**

```java
package br.com.desafio.tecnico.gestao.administracao.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

	@Bean
	public Keycloak keycloakAdmin(@Value("${gestao.keycloak-admin.server-url}") String serverUrl,
			@Value("${gestao.keycloak-admin.realm}") String realm,
			@Value("${gestao.keycloak-admin.client-id}") String clientId,
			@Value("${gestao.keycloak-admin.client-secret}") String clientSecret) {
		return KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.build();
	}

}
```

- [ ] **Step 4: Compilar e confirmar que o contexto Spring sobe**

Run: `./mvnw -q compile` — Expected: `BUILD SUCCESS`, sem erro de dependência.

Run: `./mvnw spring-boot:run` (com a Task 1 já aplicada e a stack no ar), aguarde a linha `Started
GestaoApplication`, depois `Ctrl+C`. Expected: nenhum erro de criação do bean `keycloakAdmin` no log
(a criação do `KeycloakBuilder` é local/lazy, não faz chamada de rede — só falharia aqui se alguma
propriedade estivesse ausente).

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.properties src/main/java/br/com/desafio/tecnico/gestao/administracao/config/KeycloakAdminConfig.java
git commit -m "feat: dependência keycloak-admin-client e bean do cliente administrativo"
```

---

### Task 3: `Papel`, DTOs e `AdministracaoUsuarioService` (TDD)

**Files:**
- Create: `src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/Papel.java`
- Create: `src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/UsuarioAdminDto.java`
- Create: `src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/ReatribuirPapelRequest.java`
- Create: `src/main/java/br/com/desafio/tecnico/gestao/administracao/service/AdministracaoUsuarioService.java`
- Test: `src/test/java/br/com/desafio/tecnico/gestao/administracao/service/AdministracaoUsuarioServiceTest.java`

**Interfaces:**
- Consumes: bean `Keycloak keycloakAdmin` (Task 2), propriedade `gestao.keycloak-admin.realm`.
- Produces: `AdministracaoUsuarioService.listarUsuarios(): List<UsuarioAdminDto>`,
  `AdministracaoUsuarioService.reatribuirPapel(String usuarioId, Papel novoPapel): void` — consumidos pela
  Task 4 (`AdministracaoUsuarioController`). `UsuarioAdminDto(String id, String username, String nome,
  String email, String papel)`. `ReatribuirPapelRequest(Papel papel)`.

- [ ] **Step 1: Criar o enum `Papel`**

```java
package br.com.desafio.tecnico.gestao.administracao.dto;

public enum Papel {
	ALUNO, SECRETARIA, ADMIN
}
```

- [ ] **Step 2: Criar os DTOs**

`UsuarioAdminDto.java`:
```java
package br.com.desafio.tecnico.gestao.administracao.dto;

public record UsuarioAdminDto(String id, String username, String nome, String email, String papel) {
}
```

`ReatribuirPapelRequest.java`:
```java
package br.com.desafio.tecnico.gestao.administracao.dto;

import jakarta.validation.constraints.NotNull;

public record ReatribuirPapelRequest(@NotNull Papel papel) {
}
```

- [ ] **Step 3: Escrever o teste (falhando) de `listarUsuarios`**

```java
package br.com.desafio.tecnico.gestao.administracao.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.tecnico.gestao.administracao.dto.Papel;
import br.com.desafio.tecnico.gestao.administracao.dto.UsuarioAdminDto;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministracaoUsuarioServiceTest {

	private static final String REALM = "gestao";

	@Mock
	private Keycloak keycloak;
	@Mock
	private RealmResource realmResource;
	@Mock
	private UsersResource usersResource;
	@Mock
	private UserResource userResource;
	@Mock
	private RoleMappingResource roleMappingResource;
	@Mock
	private RoleScopeResource roleScopeResource;
	@Mock
	private RolesResource rolesResource;

	private AdministracaoUsuarioService service;

	@org.junit.jupiter.api.BeforeEach
	void configurarService() {
		service = new AdministracaoUsuarioService(keycloak, REALM);
		when(keycloak.realm(REALM)).thenReturn(realmResource);
		when(realmResource.users()).thenReturn(usersResource);
	}

	@Test
	void listarUsuarios_mapeiaUsuarioComPapelRealmRole() {
		UserRepresentation representacao = new UserRepresentation();
		representacao.setId("kc-1");
		representacao.setUsername("aluno.teste");
		representacao.setFirstName("Aluno");
		representacao.setLastName("Teste");
		representacao.setEmail("aluno.teste@gestao.local");
		when(usersResource.list()).thenReturn(List.of(representacao));
		when(usersResource.get("kc-1")).thenReturn(userResource);
		when(userResource.roles()).thenReturn(roleMappingResource);
		when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
		RoleRepresentation papelAluno = new RoleRepresentation();
		papelAluno.setName("ALUNO");
		when(roleScopeResource.listAll()).thenReturn(List.of(papelAluno));

		List<UsuarioAdminDto> usuarios = service.listarUsuarios();

		assertThat(usuarios).hasSize(1);
		UsuarioAdminDto usuario = usuarios.get(0);
		assertThat(usuario.id()).isEqualTo("kc-1");
		assertThat(usuario.username()).isEqualTo("aluno.teste");
		assertThat(usuario.nome()).isEqualTo("Aluno Teste");
		assertThat(usuario.email()).isEqualTo("aluno.teste@gestao.local");
		assertThat(usuario.papel()).isEqualTo("ALUNO");
	}

	@Test
	void reatribuirPapel_removeOPapelAtualEAdicionaONovo() {
		when(usersResource.get("kc-1")).thenReturn(userResource);
		UserRepresentation representacao = new UserRepresentation();
		representacao.setId("kc-1");
		when(userResource.toRepresentation()).thenReturn(representacao);
		when(userResource.roles()).thenReturn(roleMappingResource);
		when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
		RoleRepresentation papelAtual = new RoleRepresentation();
		papelAtual.setName("ALUNO");
		when(roleScopeResource.listAll()).thenReturn(List.of(papelAtual));
		when(realmResource.roles()).thenReturn(rolesResource);
		org.keycloak.admin.client.resource.RoleResource roleResource =
				org.mockito.Mockito.mock(org.keycloak.admin.client.resource.RoleResource.class);
		when(rolesResource.get("ADMIN")).thenReturn(roleResource);
		RoleRepresentation papelNovo = new RoleRepresentation();
		papelNovo.setName("ADMIN");
		when(roleResource.toRepresentation()).thenReturn(papelNovo);

		service.reatribuirPapel("kc-1", Papel.ADMIN);

		verify(roleScopeResource).remove(List.of(papelAtual));
		verify(roleScopeResource).add(List.of(papelNovo));
	}

	@Test
	void reatribuirPapel_usuarioInexistente_lancaRecursoNaoEncontrado() {
		when(usersResource.get("kc-inexistente")).thenReturn(userResource);
		when(userResource.toRepresentation())
				.thenThrow(new jakarta.ws.rs.NotFoundException());

		assertThatThrownBy(() -> service.reatribuirPapel("kc-inexistente", Papel.ADMIN))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
```

- [ ] **Step 4: Rodar o teste e confirmar que falha**

Run: `./mvnw test -Dtest=AdministracaoUsuarioServiceTest`
Expected: FAIL — `AdministracaoUsuarioService` ainda não existe (erro de compilação).

- [ ] **Step 5: Implementar `AdministracaoUsuarioService`**

```java
package br.com.desafio.tecnico.gestao.administracao.service;

import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.desafio.tecnico.gestao.administracao.dto.Papel;
import br.com.desafio.tecnico.gestao.administracao.dto.UsuarioAdminDto;
import br.com.desafio.tecnico.gestao.errorhandling.RecursoNaoEncontradoException;

/**
 * specs/010: só reatribui papel de usuário já existente no Keycloak - não cria usuário,
 * não altera senha/enabled (D045). Um usuário só tem um dos três papéis gerenciados por
 * vez, por construção do realm (D045) - reatribuir remove o atual e adiciona o novo.
 */
@Service
public class AdministracaoUsuarioService {

	private static final Set<String> PAPEIS_GERENCIADOS = Set.of("ALUNO", "SECRETARIA", "ADMIN");

	private final Keycloak keycloakAdmin;
	private final String realm;

	public AdministracaoUsuarioService(Keycloak keycloakAdmin,
			@Value("${gestao.keycloak-admin.realm}") String realm) {
		this.keycloakAdmin = keycloakAdmin;
		this.realm = realm;
	}

	public List<UsuarioAdminDto> listarUsuarios() {
		RealmResource realmResource = keycloakAdmin.realm(realm);
		List<UserRepresentation> usuarios = realmResource.users().list();
		return usuarios.stream().map(usuario -> paraDto(realmResource, usuario)).toList();
	}

	public void reatribuirPapel(String usuarioId, Papel novoPapel) {
		RealmResource realmResource = keycloakAdmin.realm(realm);
		UserResource usuario = buscarUsuario(realmResource.users(), usuarioId);

		RoleScopeResource papeisDoUsuario = usuario.roles().realmLevel();
		List<RoleRepresentation> papeisAtuais = papeisDoUsuario.listAll().stream()
				.filter(papel -> PAPEIS_GERENCIADOS.contains(papel.getName())).toList();
		if (!papeisAtuais.isEmpty()) {
			papeisDoUsuario.remove(papeisAtuais);
		}
		RoleRepresentation novoPapelRepresentacao = realmResource.roles().get(novoPapel.name()).toRepresentation();
		papeisDoUsuario.add(List.of(novoPapelRepresentacao));
	}

	private UserResource buscarUsuario(UsersResource usersResource, String usuarioId) {
		UserResource usuario = usersResource.get(usuarioId);
		try {
			usuario.toRepresentation();
		} catch (jakarta.ws.rs.NotFoundException e) {
			throw new RecursoNaoEncontradoException("Usuário com id '" + usuarioId + "' não encontrado.");
		}
		return usuario;
	}

	private UsuarioAdminDto paraDto(RealmResource realmResource, UserRepresentation usuario) {
		List<RoleRepresentation> papeis = realmResource.users().get(usuario.getId()).roles().realmLevel().listAll();
		String papel = papeis.stream().map(RoleRepresentation::getName).filter(PAPEIS_GERENCIADOS::contains)
				.findFirst().orElse(null);
		String nome = ((usuario.getFirstName() != null ? usuario.getFirstName() : "") + " "
				+ (usuario.getLastName() != null ? usuario.getLastName() : "")).trim();
		return new UsuarioAdminDto(usuario.getId(), usuario.getUsername(), nome, usuario.getEmail(), papel);
	}

}
```

- [ ] **Step 6: Rodar o teste e confirmar que passa**

Run: `./mvnw test -Dtest=AdministracaoUsuarioServiceTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0` — `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/br/com/desafio/tecnico/gestao/administracao/dto/ src/main/java/br/com/desafio/tecnico/gestao/administracao/service/ src/test/java/br/com/desafio/tecnico/gestao/administracao/service/
git commit -m "feat: AdministracaoUsuarioService - listar usuários e reatribuir papel (D045)"
```

---

### Task 4: `AdministracaoUsuarioController` + teste de integração (autorização)

**Files:**
- Create: `src/main/java/br/com/desafio/tecnico/gestao/administracao/web/AdministracaoUsuarioController.java`
- Test: `src/test/java/br/com/desafio/tecnico/gestao/administracao/AdministracaoUsuarioControllerIntegrationTest.java`

**Interfaces:**
- Consumes: `AdministracaoUsuarioService.listarUsuarios()`/`reatribuirPapel(String, Papel)` (Task 3).
- Produces: `GET /api/admin/usuarios` → `List<UsuarioAdminDto>`; `PATCH /api/admin/usuarios/{id}/papel`
  (body `ReatribuirPapelRequest`) → 204 — consumido pelo frontend (Task 5).

- [ ] **Step 1: Escrever o teste de integração (falhando)**

```java
package br.com.desafio.tecnico.gestao.administracao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * specs/010: cobre só a borda de autorização (@PreAuthorize de classe, ADMIN-only) -
 * mesmo padrão de AlunoControllerIntegrationTest. Não testa o corpo da resposta de
 * sucesso aqui (exigiria um Keycloak real respondendo à Admin API) - esse caminho é
 * validado manualmente (seção 10 da spec) contra a stack real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdministracaoUsuarioControllerIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.docker.compose.enabled", () -> "false");
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listarSemToken_retorna401() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void listarComPapelAluno_retorna403() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios").with(aluno())).andExpect(status().isForbidden());
	}

	@Test
	void listarComPapelSecretaria_retorna403() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios").with(secretaria())).andExpect(status().isForbidden());
	}

	private static RequestPostProcessor aluno() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUNO"));
	}

	private static RequestPostProcessor secretaria() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_SECRETARIA"));
	}

}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./mvnw test -Dtest=AdministracaoUsuarioControllerIntegrationTest`
Expected: FAIL — `AdministracaoUsuarioController`/rota `/api/admin/usuarios` ainda não existe (404 em
vez dos status esperados, ou erro de compilação).

- [ ] **Step 3: Implementar o controller**

```java
package br.com.desafio.tecnico.gestao.administracao.web;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.desafio.tecnico.gestao.administracao.dto.ReatribuirPapelRequest;
import br.com.desafio.tecnico.gestao.administracao.dto.UsuarioAdminDto;
import br.com.desafio.tecnico.gestao.administracao.service.AdministracaoUsuarioService;

/**
 * specs/010: ADMIN-only, sem excecão de método (D045) - gerenciar quem tem qual papel é
 * justamente a primeira regra real que diferencia ADMIN de SECRETARIA (D011).
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "Administração")
@PreAuthorize("hasRole('ADMIN')")
public class AdministracaoUsuarioController {

	private final AdministracaoUsuarioService administracaoUsuarioService;

	public AdministracaoUsuarioController(AdministracaoUsuarioService administracaoUsuarioService) {
		this.administracaoUsuarioService = administracaoUsuarioService;
	}

	@GetMapping
	public List<UsuarioAdminDto> listar() {
		return administracaoUsuarioService.listarUsuarios();
	}

	@PatchMapping("/{id}/papel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reatribuirPapel(@PathVariable String id, @Valid @RequestBody ReatribuirPapelRequest request) {
		administracaoUsuarioService.reatribuirPapel(id, request.papel());
	}

}
```

- [ ] **Step 4: Rodar o teste e confirmar que passa**

Run: `./mvnw test -Dtest=AdministracaoUsuarioControllerIntegrationTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0` — `BUILD SUCCESS`.

- [ ] **Step 5: Rodar a suíte completa**

Run: `./mvnw clean verify`
Expected: `BUILD SUCCESS`, gate JaCoCo cumprido (o módulo `administracao` novo entra no cálculo — a
lógica real está toda em `AdministracaoUsuarioService`, já cobrida pela Task 3; `KeycloakAdminConfig`
cai na exclusão `**/config/**` já existente, D014).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/desafio/tecnico/gestao/administracao/web/ src/test/java/br/com/desafio/tecnico/gestao/administracao/AdministracaoUsuarioControllerIntegrationTest.java
git commit -m "feat: endpoints de administração de usuários/papéis (D045)"
```

---

### Task 5: Frontend — modelo, serviço e tela de listagem

**Files:**
- Create: `frontend/src/app/shared/models/usuario-admin.model.ts`
- Create: `frontend/src/app/features/administracao/usuario-admin.service.ts`
- Create: `frontend/src/app/features/administracao/usuario-admin.service.spec.ts`
- Create: `frontend/src/app/features/administracao/usuarios-lista.component.ts`

**Interfaces:**
- Consumes: `GET /api/admin/usuarios`, `PATCH /api/admin/usuarios/{id}/papel` (Task 4).
- Produces: `UsuarioAdminService.usuarios: Signal<UsuarioAdminResponse[]>`,
  `UsuarioAdminService.carregando: Signal<boolean>`, `UsuarioAdminService.carregar(): Promise<void>`,
  `UsuarioAdminService.reatribuirPapel(id: string, papel: Papel): Promise<void>` — consumido pela Task 6
  (rota/sidebar aponta para `UsuariosListaComponent`).

- [ ] **Step 1: Criar o modelo**

```typescript
export type Papel = 'ALUNO' | 'SECRETARIA' | 'ADMIN';

export interface UsuarioAdminResponse {
  id: string;
  username: string;
  nome: string;
  email: string;
  papel: Papel | null;
}

export interface ReatribuirPapelRequest {
  papel: Papel;
}
```

- [ ] **Step 2: Escrever o teste (falhando) do serviço**

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UsuarioAdminService } from './usuario-admin.service';
import { environment } from '../../../environments/environment';
import { UsuarioAdminResponse } from '../../shared/models/usuario-admin.model';

describe('UsuarioAdminService', () => {
  let service: UsuarioAdminService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/admin/usuarios`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(UsuarioAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('carrega a lista de usuarios e atualiza o signal', async () => {
    const usuarios: UsuarioAdminResponse[] = [
      { id: 'kc-1', username: 'aluno.teste', nome: 'Aluno Teste', email: 'a@a.com', papel: 'ALUNO' },
    ];
    const promise = service.carregar();
    httpMock.expectOne(baseUrl).flush(usuarios);
    await promise;
    expect(service.usuarios()).toEqual(usuarios);
  });

  it('reatribui papel via PATCH', async () => {
    const promise = service.reatribuirPapel('kc-1', 'ADMIN');
    const req = httpMock.expectOne(`${baseUrl}/kc-1/papel`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ papel: 'ADMIN' });
    req.flush(null, { status: 204, statusText: 'No Content' });
    await promise;
  });
});
```

- [ ] **Step 3: Rodar o teste e confirmar que falha**

Run: `cd frontend && npx ng test --browsers=ChromeHeadless --watch=false --include='**/usuario-admin.service.spec.ts'`
Expected: FAIL — `UsuarioAdminService` ainda não existe.

- [ ] **Step 4: Implementar o serviço**

```typescript
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Papel, UsuarioAdminResponse } from '../../shared/models/usuario-admin.model';

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/usuarios`;

  readonly usuarios = signal<UsuarioAdminResponse[]>([]);
  readonly carregando = signal(false);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const lista = await firstValueFrom(this.http.get<UsuarioAdminResponse[]>(this.baseUrl));
      this.usuarios.set(lista);
    } finally {
      this.carregando.set(false);
    }
  }

  async reatribuirPapel(id: string, papel: Papel): Promise<void> {
    await firstValueFrom(this.http.patch<void>(`${this.baseUrl}/${id}/papel`, { papel }));
  }
}
```

- [ ] **Step 5: Rodar o teste e confirmar que passa**

Run: `cd frontend && npx ng test --browsers=ChromeHeadless --watch=false --include='**/usuario-admin.service.spec.ts'`
Expected: `TOTAL: 2 SUCCESS`.

- [ ] **Step 6: Implementar a tela de listagem**

```typescript
import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuarioAdminService } from './usuario-admin.service';
import { Papel } from '../../shared/models/usuario-admin.model';
import { mensagemAmigavel } from '../../core/error-handling/problem-detail.util';

const PAPEIS: Papel[] = ['ALUNO', 'SECRETARIA', 'ADMIN'];

@Component({
  selector: 'app-usuarios-lista',
  template: `
    <div class="toolbar">
      <h1>Usuários e Papéis</h1>
    </div>

    @if (erro(); as mensagem) {
      <div class="banner banner-error">{{ mensagem }}</div>
    }

    @if (usuarioAdminService.carregando()) {
      <p class="loading">Carregando...</p>
    } @else {
      <table class="data-table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Usuário</th>
            <th>Email</th>
            <th>Papel</th>
          </tr>
        </thead>
        <tbody>
          @for (usuario of usuarioAdminService.usuarios(); track usuario.id) {
            <tr>
              <td>{{ usuario.nome }}</td>
              <td>{{ usuario.username }}</td>
              <td>{{ usuario.email }}</td>
              <td>
                <select
                  [value]="usuario.papel"
                  (change)="onPapelAlterado(usuario.id, $event)"
                  [disabled]="alterando() === usuario.id"
                >
                  @for (papel of papeis; track papel) {
                    <option [value]="papel">{{ papel }}</option>
                  }
                </select>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Nenhum usuário encontrado.</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class UsuariosListaComponent implements OnInit {
  protected readonly usuarioAdminService = inject(UsuarioAdminService);
  protected readonly erro = signal<string | null>(null);
  protected readonly alterando = signal<string | null>(null);
  protected readonly papeis = PAPEIS;

  async ngOnInit(): Promise<void> {
    try {
      await this.usuarioAdminService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    }
  }

  async onPapelAlterado(id: string, evento: Event): Promise<void> {
    const novoPapel = (evento.target as HTMLSelectElement).value as Papel;
    this.erro.set(null);
    this.alterando.set(id);
    try {
      await this.usuarioAdminService.reatribuirPapel(id, novoPapel);
      await this.usuarioAdminService.carregar();
    } catch (e) {
      this.erro.set(mensagemAmigavel(e as HttpErrorResponse));
    } finally {
      this.alterando.set(null);
    }
  }
}
```

- [ ] **Step 7: Build**

Run: `cd frontend && npx ng build`
Expected: `Application bundle generation complete`, sem erros.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/shared/models/usuario-admin.model.ts frontend/src/app/features/administracao/
git commit -m "feat: tela de administração de usuários/papéis (frontend)"
```

---

### Task 6: Rota, sidebar e verificação manual completa

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/app.html`
- Create: `e2e/administracao-papel-flow.sh`

**Interfaces:**
- Consumes: `UsuariosListaComponent` (Task 5), `currentUser.temPapel('ADMIN')` (já existente,
  `current-user.ts`).
- Produces: rota `/administracao/usuarios` navegável; item de sidebar "Usuários e Papéis" visível só
  para ADMIN.

- [ ] **Step 1: Adicionar a rota em `app.routes.ts`**

Adicione a constante e a rota (após `const ALUNO = ['ALUNO'];` e antes da rota `acesso-negado`,
respectivamente):

```typescript
const ADMIN = ['ADMIN'];
```

```typescript
  {
    path: 'administracao/usuarios',
    loadComponent: () =>
      import('./features/administracao/usuarios-lista.component').then((m) => m.UsuariosListaComponent),
    canActivate: [roleGuard],
    data: { papeis: ADMIN },
  },
```

- [ ] **Step 2: Adicionar a seção "Administração" em `app.html`**

Insira, dentro de `<nav>`, depois do bloco `@if (currentUser.temPapel('ALUNO'))` (que termina na linha
com `}` antes de `</nav>`) e antes de `</nav>`:

```html
      @if (currentUser.temPapel('ADMIN')) {
        <div class="nav-section">
          <div class="nav-label">Administração</div>
          <a class="nav-item" routerLink="/administracao/usuarios" routerLinkActive="active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l7 3v6c0 5-3 8-7 9-4-1-7-4-7-9V6l7-3z"/><polyline points="9 12 11 14 15 10"/></svg>
            <span>Usuários e Papéis</span>
          </a>
        </div>
      }
```

- [ ] **Step 3: Build e testes do frontend**

Run: `cd frontend && npx ng build && npx ng test --browsers=ChromeHeadless --watch=false`
Expected: build sem erros; `TOTAL: 25 SUCCESS` (23 já existentes + 2 novos do
`usuario-admin.service.spec.ts`).

- [ ] **Step 4: Commit do frontend**

```bash
git add frontend/src/app/app.routes.ts frontend/src/app/app.html
git commit -m "feat: rota e item de sidebar da administração de usuários (D046)"
```

- [ ] **Step 5: Criar o script E2E**

```bash
#!/usr/bin/env bash
# e2e/administracao-papel-flow.sh
# specs/010: ADMIN reatribui o papel de um usuário e confirma via novo login que o
# token reflete o novo papel. Requer a stack completa no ar (compose + backend em :8080
# + Keycloak em :8081) e as credenciais de teste do README.
set -euo pipefail

API="${API_BASE_URL:-http://localhost:8080}"
KEYCLOAK="${KEYCLOAK_BASE_URL:-http://localhost:8081}"
REALM="gestao"

token() {
  curl -s -X POST "$KEYCLOAK/realms/$REALM/protocol/openid-connect/token" \
    -d "client_id=gestao-frontend" -d "grant_type=password" \
    -d "username=$1" -d "password=$2" | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])"
}

echo "1) Login como admin.teste"
ADMIN_TOKEN=$(token admin.teste admin123)

echo "2) Listar usuarios via GET /api/admin/usuarios"
USUARIOS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$API/api/admin/usuarios")
SECRETARIA_ID=$(echo "$USUARIOS" | python3 -c "
import sys, json
for u in json.load(sys.stdin):
    if u['username'] == 'secretaria.teste':
        print(u['id']); break
")
if [ -z "$SECRETARIA_ID" ]; then
  echo "FALHA: secretaria.teste nao encontrada na listagem"; exit 1
fi
echo "OK - secretaria.teste id=$SECRETARIA_ID"

echo "3) Reatribuir papel de secretaria.teste para ADMIN"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"papel":"ADMIN"}' "$API/api/admin/usuarios/$SECRETARIA_ID/papel")
if [ "$STATUS" != "204" ]; then
  echo "FALHA: PATCH retornou $STATUS, esperado 204"; exit 1
fi
echo "OK - papel reatribuido (204)"

echo "4) Novo login de secretaria.teste deve trazer o papel ADMIN no token"
NOVO_TOKEN=$(token secretaria.teste secretaria123)
PAPEIS=$(echo "$NOVO_TOKEN" | python3 -c "
import sys, json, base64
token = sys.stdin.read().strip()
payload = token.split('.')[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))['realm_access']['roles'])
")
if [[ "$PAPEIS" != *"ADMIN"* ]]; then
  echo "FALHA: token de secretaria.teste nao contem ADMIN apos reatribuicao: $PAPEIS"; exit 1
fi
echo "OK - novo token de secretaria.teste contem ADMIN"

echo "5) Reverter: devolver secretaria.teste para o papel SECRETARIA (deixa o realm como estava)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"papel":"SECRETARIA"}' "$API/api/admin/usuarios/$SECRETARIA_ID/papel")
if [ "$STATUS" != "204" ]; then
  echo "FALHA: reversao retornou $STATUS, esperado 204"; exit 1
fi
echo "OK - papel revertido para SECRETARIA"

echo "=================================="
echo "Todas as validacoes passaram."
```

- [ ] **Step 6: Tornar o script executável e rodar contra a stack real**

Run:
```bash
chmod +x e2e/administracao-papel-flow.sh
docker compose up -d
# (backend já deve estar rodando via ./mvnw spring-boot:run, como nos demais e2e/*.sh)
bash e2e/administracao-papel-flow.sh
```
Expected: as 5 mensagens `OK -` impressas em sequência, terminando em "Todas as validacoes passaram."

- [ ] **Step 7: Adicionar o script ao workflow de CI**

Em `.github/workflows/ci.yml`, no job `build`, adicione uma nova etapa depois de "E2E fluxo de Matrícula":

```yaml
      - name: E2E administracao de usuarios/papeis (specs/010)
        run: bash e2e/administracao-papel-flow.sh
```

- [ ] **Step 8: Gate final e commit**

Run: `./mvnw clean verify` — Expected: `BUILD SUCCESS`.

```bash
git add e2e/administracao-papel-flow.sh .github/workflows/ci.yml
git commit -m "test: e2e de reatribuição de papel (specs/010)"
```

---

## Self-Review

**1. Cobertura da spec:** endpoints (Task 4) ✓; integração Keycloak/service account (Task 1) ✓; escopo
"só reatribuir papel" (Task 3, sem criar/desabilitar usuário) ✓; ADMIN-only (Task 4, `@PreAuthorize` de
classe + teste 403 para ALUNO/SECRETARIA) ✓; frontend + sidebar (Tasks 5-6) ✓; e2e (Task 6) ✓; nenhuma
exceção nova criada (reuso de `RecursoNaoEncontradoException` + enum, conforme Global Constraints) ✓.

**2. Placeholders:** nenhum "TBD"/"TODO" — todo código Java/TS acima é conteúdo final.

**3. Consistência de tipos:** `Papel` (enum Java, `ALUNO/SECRETARIA/ADMIN`) e `Papel` (union type TS,
mesmos 3 valores) usados de forma consistente em todas as tasks; `UsuarioAdminDto`/`UsuarioAdminResponse`
com os mesmos 5 campos (`id, username, nome, email, papel`) nos dois lados; assinatura
`reatribuirPapel(id: string, papel: Papel)` idêntica no service e no uso do componente (Task 5/6).

## Execution Handoff

Plan completo e salvo em `docs/superpowers/plans/2026-07-12-administracao-usuarios-papeis.md`. Duas
opções de execução:

**1. Subagent-Driven (recomendado)** — dispato um subagent fresco por task (backend via
`spring-boot-engineer`, frontend via `angular-architect`), com revisão entre elas.

**2. Execução inline** — executo as 6 tasks nesta sessão via `executing-plans`, em lote, com checkpoints
para revisão.

Qual abordagem?
