package cl.joaquin.siga.DTOs.AcademicPeriodDTO;

import java.util.List;

public record PeriodCloseResponseDTO(
        AcademicPeriodResponseDTO period,
        int recordsCreated,
        List<Long> graduatedStudentIds,
        List<Long> eliminatedStudentIds
) {
}
