package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "study_plan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_study_plan_career_code", columnNames = {"career_id", "code"}),
                @UniqueConstraint(name = "uk_study_plan_current_career", columnNames = {"current_career_id"})
        }
)
public class StudyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "career_id", nullable = false)
    private Long careerId;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyPlanStatus status;

    // Igual a careerId mientras el plan es CURRENT y null en otro caso. Postgres permite varios NULL en
    // una unique, así que esto da "un solo plan vigente por carrera" sin índice parcial
    @Column(name = "current_career_id")
    private Long currentCareerId;
}
