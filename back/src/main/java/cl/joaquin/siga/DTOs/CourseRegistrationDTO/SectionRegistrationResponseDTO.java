package cl.joaquin.siga.DTOs.CourseRegistrationDTO;

import cl.joaquin.siga.Entities.University.CourseResult;

import java.math.BigDecimal;

// un inscrito de la sección, con los datos del estudiante que el docente necesita para identificarlo
public record SectionRegistrationResponseDTO(
        Long id,
        Long studentId,
        String studentRun,
        String studentFullName,
        BigDecimal grade,
        CourseResult result
) {
}
