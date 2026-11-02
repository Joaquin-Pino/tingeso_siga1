package cl.joaquin.siga.DTOs.SectionDTO;

import cl.joaquin.siga.Entities.University.WeekDay;
import jakarta.validation.constraints.NotNull;

public record ScheduleBlockDTO(
        @NotNull WeekDay day,
        @NotNull Integer module
) {
}
