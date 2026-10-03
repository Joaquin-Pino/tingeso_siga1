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
        name = "section",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_section_subject_academic_period",
                columnNames = {"subject_id", "academic_period_id"}
        )
)
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer enrolledCount;

    @Version
    private Long version;
}
