package cl.joaquin.siga.DTOs.CourseRegistrationDTO;

import cl.joaquin.siga.Entities.University.CourseResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseRegistrationResponseDTO(
        Long id,
        Long studentId,
        Long sectionId,
        Long subjectId,
        Long academicPeriodId,
        LocalDate registrationDate,
        BigDecimal grade,
        CourseResult result
) {
}
