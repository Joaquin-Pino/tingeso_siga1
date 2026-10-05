package cl.joaquin.siga.DTOs.CareerDTO;

import cl.joaquin.siga.Entities.University.CareerStatus;

public record CareerResponseDTO(
        Long id,
        String code,
        String name,
        String description,
        CareerStatus status,
        Integer startYear,
        Integer vacancies
) {
}
