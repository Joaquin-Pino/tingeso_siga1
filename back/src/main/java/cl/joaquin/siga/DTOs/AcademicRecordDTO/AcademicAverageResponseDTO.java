package cl.joaquin.siga.DTOs.AcademicRecordDTO;

import java.math.BigDecimal;

// average es null mientras el estudiante no tenga historial
public record AcademicAverageResponseDTO(
        Long studentId,
        BigDecimal average,
        int subjectsCount,
        int totalCredits
) {
}
