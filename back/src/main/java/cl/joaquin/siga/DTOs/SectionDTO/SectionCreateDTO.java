package cl.joaquin.siga.DTOs.SectionDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// sin enrolledCount: parte en 0 y solo lo mueven las inscripciones.
// List y no Set: así el Service detecta los bloques duplicados en vez de descartarlos en silencio
public record SectionCreateDTO(
        @NotNull Long subjectId,
        @NotNull Long academicPeriodId,
        @NotNull Long teacherId,
        @NotNull @Min(1) @Max(60) Integer capacity,
        @NotNull List<@Valid @NotNull ScheduleBlockDTO> scheduleBlocks
) {
}
