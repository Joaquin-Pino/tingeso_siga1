package cl.joaquin.siga.DTOs.CareerDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CareerCreateDTO(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @Positive Integer startYear,
        @NotNull @PositiveOrZero Integer vacancies
) {
}
