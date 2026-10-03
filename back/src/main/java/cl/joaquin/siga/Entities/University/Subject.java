package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subject" ,
        uniqueConstraints = @UniqueConstraint(
            name = "uk_subject_study_plan_code",
            columnNames = {"study_plan_id", "code"}
        )
)
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_plan_id", nullable = false)
    private Long studyPlanId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer semester;

    @Embedded
    private WeeklyHours weeklyHours;

    @Column(nullable = false)
    private Integer credits;
}
