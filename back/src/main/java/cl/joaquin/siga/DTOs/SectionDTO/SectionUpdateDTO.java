package cl.joaquin.siga.DTOs.SectionDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// sin subjectId ni academicPeriodId: cambiarlos sería otra sección distinta
public record SectionUpdateDTO(
        @NotNull Long teacherId,
        @NotNull @Min(1) @Max(60) Integer capacity,
        @NotNull List<@Valid @NotNull ScheduleBlockDTO> scheduleBlocks
) {
}
