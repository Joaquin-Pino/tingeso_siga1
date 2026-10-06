package cl.joaquin.siga.DTOs.SubjectDTO;

import java.util.List;

public record SubjectResponseDTO(
        Long id,
        Long studyPlanId,
        String code,
        String name,
        Integer semester,
        Integer theoryHours,
        Integer exerciseHours,
        Integer labHours,
        Integer credits,
        List<Long> prerequisiteIds
) {
}
