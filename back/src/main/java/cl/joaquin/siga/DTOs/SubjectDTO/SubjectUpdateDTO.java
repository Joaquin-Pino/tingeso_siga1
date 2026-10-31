package cl.joaquin.siga.DTOs.SubjectDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

// sin studyPlanId: cambiar de plan dejaría los prerrequisitos apuntando a otro plan
public record SubjectUpdateDTO(
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
