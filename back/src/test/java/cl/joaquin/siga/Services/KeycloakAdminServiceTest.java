package cl.joaquin.siga.Services;

import cl.joaquin.siga.Exceptions.KeycloakException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminServiceTest {

    private static final String BASE = "http://kc";
    private static final String TOKEN_URL = BASE + "/realms/siga-realm/protocol/openid-connect/token";
    private static final String USERS_URL = BASE + "/admin/realms/siga-realm/users";
    private static final String TOKEN_BODY = "{\"access_token\": \"admin-token\"}";

    private MockRestServiceServer server;
    private KeycloakAdminService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KeycloakAdminService(builder, BASE, "siga-realm", "siga-backend", "secret");
    }

    private void expectToken() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andRespond(withSuccess(TOKEN_BODY, MediaType.APPLICATION_JSON));
    }

    @Test
    void createUser_createsAccountWithTemporaryPasswordAndRole_returnsId() {
        expectToken();
        server.expect(requestTo(USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer admin-token"))
                .andExpect(jsonPath("$.username").value("a@x.cl"))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Pérez Soto"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(true))
                .andRespond(withStatus(HttpStatus.CREATED).header("Location", USERS_URL + "/new-id"));
        server.expect(requestTo(BASE + "/admin/realms/siga-realm/roles/STUDENT"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\": \"r1\", \"name\": \"STUDENT\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL + "/new-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("STUDENT"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertEquals("new-id", service.createUser("a@x.cl", "Ana Pérez Soto", "Temp1234", "STUDENT"));
        server.verify();
    }

    @Test
    void createUser_singleWordName_usesPlaceholderLastName() {
        expectToken();
        server.expect(requestTo(USERS_URL))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("-"))
                .andRespond(withStatus(HttpStatus.CREATED).header("Location", USERS_URL + "/new-id"));
        server.expect(requestTo(BASE + "/admin/realms/siga-realm/roles/STUDENT"))
                .andRespond(withSuccess("{\"name\": \"STUDENT\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL + "/new-id/role-mappings/realm"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        service.createUser("a@x.cl", "Ana", "Temp1234", "STUDENT");
        server.verify();
    }

    @Test
    void createUser_emailAlreadyInKeycloak_throwsIllegalArgument() {
        expectToken();
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(IllegalArgumentException.class, () -> service.createUser("a@x.cl", "Ana", "Temp1234", "STUDENT"));
    }

    @Test
    void createUser_passwordRejected_throwsIllegalArgument() {
        expectToken();
        server.expect(requestTo(USERS_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"errorMessage\": \"invalid password\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(IllegalArgumentException.class, () -> service.createUser("a@x.cl", "Ana", "x", "STUDENT"));
    }

    @Test
    void createUser_tokenDenied_throwsKeycloakException() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(KeycloakException.class, () -> service.createUser("a@x.cl", "Ana", "Temp1234", "STUDENT"));
    }

    @Test
    void createUser_roleAssignmentFails_deletesTheAccount() {
        expectToken();
        server.expect(requestTo(USERS_URL))
                .andRespond(withStatus(HttpStatus.CREATED).header("Location", USERS_URL + "/new-id"));
        server.expect(requestTo(BASE + "/admin/realms/siga-realm/roles/STUDENT"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectToken();
        server.expect(requestTo(USERS_URL + "/new-id"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThrows(KeycloakException.class, () -> service.createUser("a@x.cl", "Ana", "Temp1234", "STUDENT"));
        server.verify();
    }

    @Test
    void deleteUser_keycloakDown_doesNotThrow() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        service.deleteUser("some-id");
    }
}
