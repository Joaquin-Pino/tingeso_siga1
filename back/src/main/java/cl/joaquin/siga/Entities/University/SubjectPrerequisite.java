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
        name = "subject_prerequisite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subject_prerequisite_subject_prerequisite",
                columnNames = {"subject_id", "prerequisite_subject_id"}
        )
)
public class SubjectPrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "prerequisite_subject_id", nullable = false)
    private Long prerequisiteSubjectId;
}
