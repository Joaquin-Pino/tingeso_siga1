package cl.joaquin.siga.DTOs.SubjectDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record SubjectCreateDTO(
        @NotNull Long studyPlanId,
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Integer semester,
        @NotNull Integer theoryHours,
        @NotNull Integer exerciseHours,
        @NotNull Integer labHours,
        @NotNull Integer credits,
        Set<@NotNull Long> prerequisiteIds
) {
}
