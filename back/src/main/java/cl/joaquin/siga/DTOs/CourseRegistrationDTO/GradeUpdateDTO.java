package cl.joaquin.siga.DTOs.CourseRegistrationDTO;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeUpdateDTO(
        @NotNull BigDecimal grade
) {
}
