package cl.joaquin.siga.DTOs.StudyPlanDTO;

import cl.joaquin.siga.Entities.University.StudyPlanStatus;

public record StudyPlanResponseDTO(
        Long id,
        Long careerId,
        String code,
        StudyPlanStatus status
) {
}
