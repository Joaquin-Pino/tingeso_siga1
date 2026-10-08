package cl.joaquin.siga.DTOs.TeacherDTO;

import jakarta.validation.constraints.NotNull;

import cl.joaquin.siga.Entities.People.TeacherStatus;

public record TeacherStatusUpdateDTO(@NotNull TeacherStatus status) {
}
