package cl.joaquin.siga.Entities.People;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "teaching_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_teaching_record_section",
                columnNames = "section_id"
        )
)
public class TeachingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teacherId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long academicPeriodId;

    @Column(nullable = false)
    private String subjectCode;

    @Column(nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private Integer subjectSemester;

    @Column(nullable = false)
    private String periodCode;

    @Column(nullable = false)
    private Integer enrolledCount;
}
