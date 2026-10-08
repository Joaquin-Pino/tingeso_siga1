package cl.joaquin.siga.DTOs.TeacherDTO;

import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.People.TeacherStatus;

public record TeacherResponseDTO(
        Long id,
        String run,
        String fullName,
        String email,
        String professionalTitle,
        AcademicDegree academicDegree,
        TeacherStatus status
) {
}
