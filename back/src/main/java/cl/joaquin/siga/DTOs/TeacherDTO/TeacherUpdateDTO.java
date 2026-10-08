package cl.joaquin.siga.DTOs.TeacherDTO;

import cl.joaquin.siga.Entities.People.AcademicDegree;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// sin run (identidad del docente) ni estado (tiene su propio endpoint y reglas)
public record TeacherUpdateDTO(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String professionalTitle,
        @NotNull AcademicDegree academicDegree
) {
}
