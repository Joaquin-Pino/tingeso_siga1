package cl.joaquin.siga.DTOs.AcademicPeriodDTO;

import cl.joaquin.siga.Entities.University.Semester;
import jakarta.validation.constraints.NotNull;

// sin status: el período siempre se crea OPEN y solo el cierre lo pasa a CLOSED
public record AcademicPeriodCreateDTO(
        @NotNull Integer year,
        @NotNull Semester semester
) {
}
