package cl.joaquin.siga.Exceptions;

// Keycloak no respondió o falló al crear/borrar un usuario: es un problema del servicio externo, no de la petición
public class KeycloakException extends RuntimeException {

    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeycloakException(String message) {
        super(message);
    }
}
