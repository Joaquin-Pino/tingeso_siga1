package cl.joaquin.siga.DTOs.AcademicPeriodDTO;

import cl.joaquin.siga.Entities.University.Semester;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// sin status: el período siempre se crea OPEN y solo el cierre lo pasa a CLOSED
public record AcademicPeriodCreateDTO(
        @NotNull @Positive Integer year,
        @NotNull Semester semester
) {
}
