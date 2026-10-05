package cl.joaquin.siga.DTOs.StudentDTO;

import jakarta.validation.constraints.NotNull;

import cl.joaquin.siga.Entities.People.StudentStatus;

public record StudentStatusUpdateDTO(@NotNull StudentStatus status) {
}
