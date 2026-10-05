package cl.joaquin.siga.DTOs.StudentDTO;

import jakarta.validation.constraints.NotNull;

public record StudentCareerUpdateDTO(@NotNull Long careerId) {
}
