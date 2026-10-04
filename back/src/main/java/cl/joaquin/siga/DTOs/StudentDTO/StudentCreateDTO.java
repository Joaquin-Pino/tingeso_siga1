package cl.joaquin.siga.DTOs.StudentDTO;

public record StudentCreateDTO(
        String keycloakId,
        String run,
        String fullName,
        String email,
        Long careerId
) {
}
