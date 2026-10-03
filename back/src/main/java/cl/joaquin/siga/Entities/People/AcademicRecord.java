package cl.joaquin.siga.Entities.People;

import cl.joaquin.siga.Entities.University.CourseResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "academic_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_record_course_registration",
                columnNames = "course_registration_id"
        )
)
public class AcademicRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(name = "course_registration_id", nullable = false)
    private Long courseRegistrationId;

    @Column(nullable = false)
    private String subjectCode;

    @Column(nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private Integer subjectSemester;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private String periodCode;

    @Column(nullable = false)
    private BigDecimal finalGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseResult result;
}
