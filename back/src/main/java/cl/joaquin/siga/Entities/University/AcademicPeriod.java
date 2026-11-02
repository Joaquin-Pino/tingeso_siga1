package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "academic_period",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_academic_period_year_semester", columnNames = {"year", "semester"}),
                @UniqueConstraint(name = "uk_academic_period_open_marker", columnNames = {"open_marker"})
        })
public class AcademicPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodStatus status;

    // 1 mientras el período es OPEN y null en otro caso. Postgres permite varios NULL en una unique, así que esto
    // da "un solo período abierto a la vez" sin índice parcial (mismo truco que StudyPlan.currentCareerId)
    @Column(name = "open_marker")
    private Integer openMarker;
}
