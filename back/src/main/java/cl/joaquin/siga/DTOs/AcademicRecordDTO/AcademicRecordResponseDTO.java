package cl.joaquin.siga.DTOs.AcademicRecordDTO;

import cl.joaquin.siga.Entities.University.CourseResult;

import java.math.BigDecimal;

public record AcademicRecordResponseDTO(
        Long id,
        Long subjectId,
        Long academicPeriodId,
        String subjectCode,
        String subjectName,
        Integer subjectSemester,
        Integer credits,
        String periodCode,
        BigDecimal finalGrade,
        CourseResult result
) {
}
