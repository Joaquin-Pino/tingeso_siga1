package cl.joaquin.siga.DTOs.StudentDTO;

import cl.joaquin.siga.Entities.People.StudentStatus;

public record StudentResponseDTO(
        Long id,
        String run,
        String fullName,
        String email,
        Long careerId,
        Long studyPlanId,
        StudentStatus status
) {
}
