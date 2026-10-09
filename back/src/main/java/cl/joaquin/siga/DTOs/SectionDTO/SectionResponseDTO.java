package cl.joaquin.siga.DTOs.SectionDTO;

import java.util.List;

public record SectionResponseDTO(
        Long id,
        Long subjectId,
        Long academicPeriodId,
        Long teacherId,
        Integer capacity,
        Integer enrolledCount,
        List<ScheduleBlockDTO> scheduleBlocks
) {
}
