package cl.joaquin.siga.DTOs.StudentDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentCreateDTO(
        // contraseña temporal para la cuenta de Keycloak, que el Service crea; no se guarda en la app
        @NotBlank String temporaryPassword,
        @NotBlank String run,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull Long careerId
) {
}
