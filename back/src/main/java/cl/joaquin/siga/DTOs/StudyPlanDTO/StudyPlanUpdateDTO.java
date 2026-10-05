package cl.joaquin.siga.DTOs.StudyPlanDTO;

import jakarta.validation.constraints.NotBlank;

public record StudyPlanUpdateDTO(
        @NotBlank String code
) {
}
