package cl.joaquin.siga.DTOs.CareerDTO;

import jakarta.validation.constraints.NotNull;

import cl.joaquin.siga.Entities.University.CareerStatus;

public record CareerStatusUpdateDTO(@NotNull CareerStatus status) {
}
