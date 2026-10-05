package cl.joaquin.siga.DTOs.CareerDTO;

public record CareerUpdateDTO(
        String name,
        String description,
        Integer startYear,
        Integer vacancies
) {
}
