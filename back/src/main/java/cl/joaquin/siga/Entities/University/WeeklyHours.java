package cl.joaquin.siga.Entities.University;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyHours {

    @Column(name = "theory_hours", nullable = false)
    private Integer theoryHours;

    @Column(name = "exercise_hours", nullable = false)
    private Integer exerciseHours;

    @Column(name = "lab_hours", nullable = false)
    private Integer labHours;
}
