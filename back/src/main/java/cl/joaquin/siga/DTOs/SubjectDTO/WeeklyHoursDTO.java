package cl.joaquin.siga.DTOs.SubjectDTO;

import jakarta.validation.constraints.NotNull;

// TEL: orden Teoría-Ejercicios-Laboratorio (ej. 2-2-2). Los rangos los valida SubjectService
public record WeeklyHoursDTO(
        @NotNull Integer theoryHours,
        @NotNull Integer exerciseHours,
        @NotNull Integer labHours
) {
}
