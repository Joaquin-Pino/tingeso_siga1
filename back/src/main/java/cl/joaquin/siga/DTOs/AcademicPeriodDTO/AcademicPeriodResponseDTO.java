package cl.joaquin.siga.DTOs.AcademicPeriodDTO;

import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Semester;

public record AcademicPeriodResponseDTO(
        Long id,
        Integer year,
        Semester semester,
        String code,
        PeriodStatus status
) {
}
