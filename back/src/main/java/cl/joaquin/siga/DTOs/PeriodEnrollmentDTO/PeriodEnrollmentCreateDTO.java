package cl.joaquin.siga.DTOs.PeriodEnrollmentDTO;

import jakarta.validation.constraints.NotNull;

// el período va en la URL y la fecha la pone el Service
public record PeriodEnrollmentCreateDTO(
        @NotNull Long studentId
) {
}
