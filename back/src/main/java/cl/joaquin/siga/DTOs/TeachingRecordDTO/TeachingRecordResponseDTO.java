package cl.joaquin.siga.DTOs.TeachingRecordDTO;

public record TeachingRecordResponseDTO(
        Long id,
        Long sectionId,
        Long subjectId,
        Long academicPeriodId,
        String subjectCode,
        String subjectName,
        Integer subjectSemester,
        String periodCode,
        Integer enrolledCount
) {
}
