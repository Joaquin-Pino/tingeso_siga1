package cl.joaquin.siga.Services;

import cl.joaquin.siga.Exceptions.KeycloakException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;

// Crea, actualiza el correo y borra las cuentas de Keycloak de estudiantes y docentes mediante la Admin API.
// El back usa un cliente confidencial con service account (siga-backend) con los roles
// manage-users, view-users y view-realm de realm-management. La app no guarda contraseñas:
// la temporal viaja a Keycloak y este obliga a cambiarla en el primer login.
@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminService(RestClient.Builder restClientBuilder,
                                @Value("${keycloak.admin.base-url}") String baseUrl,
                                @Value("${keycloak.admin.realm}") String realm,
                                @Value("${keycloak.admin.client-id}") String clientId,
                                @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // crea el usuario (login = correo) con contraseña temporal y el rol de realm indicado; devuelve su id (el sub del JWT)
    public String createUser(String email, String fullName, String temporaryPassword, String role) {
        String token = requestAdminToken();

        String[] names = splitName(fullName);
        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "firstName", names[0],
                "lastName", names[1],
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", temporaryPassword, "temporary", true)));

        ResponseEntity<Void> response;
        try {
            response = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new IllegalArgumentException("Ya existe un usuario en Keycloak con el correo: " + email);
        } catch (HttpClientErrorException.BadRequest e) {
            // típicamente la contraseña no cumple la política del realm
            throw new IllegalArgumentException("Keycloak rechazó la cuenta: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new KeycloakException("No se pudo crear el usuario en Keycloak", e);
        }

        String userId = extractId(response.getHeaders().getLocation());
        try {
            assignRealmRole(userId, role, token);
        } catch (RuntimeException e) {
            deleteUser(userId); // no dejar una cuenta sin rol
            throw e;
        }
        return userId;
    }

    // cambia el login (username = correo) y el correo de una cuenta existente
    public void updateEmail(String userId, String email) {
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + requestAdminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", email, "email", email))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new IllegalArgumentException("Ya existe un usuario en Keycloak con el correo: " + email);
        } catch (RestClientException e) {
            throw new KeycloakException("No se pudo actualizar el correo en Keycloak", e);
        }
    }

    // compensación: se usa cuando la BD rechaza al estudiante/docente después de crear su cuenta. No lanza
    public void deleteUser(String userId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + requestAdminToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.error("No se pudo borrar el usuario {} de Keycloak; quedó huérfano", userId, e);
        }
    }

    private void assignRealmRole(String userId, String role, String token) {
        try {
            Map<String, Object> roleRepresentation = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{role}", realm, role)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new KeycloakException("No se pudo asignar el rol " + role + " en Keycloak", e);
        }
    }

    private String requestAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            if (response == null || response.get("access_token") == null) {
                throw new KeycloakException("Keycloak no entregó un token de administración");
            }
            return response.get("access_token").toString();
        } catch (RestClientException e) {
            throw new KeycloakException("No se pudo autenticar el backend contra Keycloak", e);
        }
    }

    // Keycloak exige nombre y apellido para considerar el perfil completo
    private static String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[]{trimmed, "-"};
        }
        return new String[]{trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }

    private static String extractId(URI location) {
        if (location == null) {
            throw new KeycloakException("Keycloak no devolvió el id del usuario creado");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
