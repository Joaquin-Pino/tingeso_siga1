package cl.joaquin.siga.DTOs.StudentDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentCreateDTO(
        @NotBlank String keycloakId,
        @NotBlank String run,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull Long careerId
) {
}
