package cl.joaquin.siga.DTOs.SectionDTO;

import cl.joaquin.siga.Entities.University.WeekDay;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleBlockDTO(
        @NotNull WeekDay day,
        @NotNull @Min(1) @Max(6) Integer module
) {
}
