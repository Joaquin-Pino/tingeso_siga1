package cl.joaquin.siga.DTOs.StudyPlanDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudyPlanCreateDTO(
        @NotNull Long careerId,
        @NotBlank String code
) {
}
