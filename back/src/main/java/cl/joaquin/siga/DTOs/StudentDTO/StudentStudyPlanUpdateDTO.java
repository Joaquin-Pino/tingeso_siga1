package cl.joaquin.siga.DTOs.StudentDTO;

import jakarta.validation.constraints.NotNull;

public record StudentStudyPlanUpdateDTO(@NotNull Long studyPlanId) {
}
