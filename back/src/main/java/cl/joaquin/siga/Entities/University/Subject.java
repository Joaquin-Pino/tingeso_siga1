package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    @Column(name = "code", nullable = false)
    private Long code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Semester semester;

    @Embedded
    private Tel tel;

    @Column(nullable = false)
    private long sct;
}
