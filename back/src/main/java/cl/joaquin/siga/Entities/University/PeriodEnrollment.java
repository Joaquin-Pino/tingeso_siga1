package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "period_enrollment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_matriculation_student_period",
                columnNames = {"student_id", "academic_period_id"}
        )
)
public class PeriodEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(nullable = false)
    private LocalDate matriculationDate;
}