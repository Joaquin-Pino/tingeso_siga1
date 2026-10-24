package cl.joaquin.siga.DTOs.TeacherDTO;

import cl.joaquin.siga.Entities.People.AcademicDegree;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// sin estado: el docente siempre queda ACTIVE al registrarse (lo asigna el Service)
public record TeacherCreateDTO(
        // contraseña temporal para la cuenta de Keycloak, que el Service crea; no se guarda en la app
        @NotBlank String temporaryPassword,
        @NotBlank String run,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String professionalTitle,
        @NotNull AcademicDegree academicDegree
) {
}
