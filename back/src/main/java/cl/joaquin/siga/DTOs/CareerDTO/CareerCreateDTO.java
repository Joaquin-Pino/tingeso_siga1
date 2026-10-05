package cl.joaquin.siga.DTOs.CareerDTO;

public record CareerCreateDTO(
        String code,
        String name,
        String description,
        Integer startYear,
        Integer vacancies
) {
}
