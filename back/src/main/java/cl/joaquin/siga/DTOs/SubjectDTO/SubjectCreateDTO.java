package cl.joaquin.siga.DTOs.SubjectDTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SubjectCreateDTO(
        @NotNull Long studyPlanId,
        @NotBlank String code,
        @NotBlank String name,
        @NotNull @Min(1) @Max(4) Integer semester,
        @NotNull @PositiveOrZero Integer theoryHours,
        @NotNull @PositiveOrZero Integer exerciseHours,
        @NotNull @PositiveOrZero Integer labHours,
        @NotNull @Min(1) @Max(7) Integer credits,
        @Size(max = 3) Set<@NotNull Long> prerequisiteIds
) {
}
