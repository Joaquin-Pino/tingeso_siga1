package cl.joaquin.siga.DTOs.StudentDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// sin run (identidad del estudiante), ni carrera/plan/estado (tienen sus propios endpoints y reglas)
public record StudentUpdateDTO(
        @NotBlank String fullName,
        @NotBlank @Email String email
) {
}
