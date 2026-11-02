package cl.joaquin.siga.DTOs.CareerDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CareerCreateDTO(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Integer startYear,
        @NotNull Integer vacancies
) {
}
