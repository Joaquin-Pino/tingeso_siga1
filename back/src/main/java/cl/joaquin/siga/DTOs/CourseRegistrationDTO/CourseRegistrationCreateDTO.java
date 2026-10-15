package cl.joaquin.siga.DTOs.CourseRegistrationDTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// el lote completo de secciones a inscribir; el período se deduce de las secciones.
// List y no Set: así el Service detecta los ids repetidos en vez de descartarlos en silencio
public record CourseRegistrationCreateDTO(
        @NotEmpty List<@NotNull Long> sectionIds
) {
}
