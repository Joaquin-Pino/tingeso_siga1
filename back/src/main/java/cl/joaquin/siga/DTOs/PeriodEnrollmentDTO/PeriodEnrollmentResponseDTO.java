package cl.joaquin.siga.DTOs.PeriodEnrollmentDTO;

import java.time.LocalDate;

public record PeriodEnrollmentResponseDTO(
        Long id,
        Long studentId,
        Long academicPeriodId,
        LocalDate enrollmentDate
) {
}
