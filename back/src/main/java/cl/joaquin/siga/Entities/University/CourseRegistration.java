package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "course_registration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_registration_student_subject_period",
                columnNames = {"student_id", "subject_id", "academic_period_id"}
        )
)
public class CourseRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long sectionId;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(nullable = false)
    private LocalDate registrationDate;

    private BigDecimal grade;

    @Enumerated(EnumType.STRING)
    private CourseResult result;
}
