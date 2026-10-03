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
        name = "teacher",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teacher_run", columnNames = "run"),
                @UniqueConstraint(name = "uk_teacher_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_teacher_keycloak_id", columnNames = "keycloak_id")
        }
)
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", nullable = false)
    private String keycloakId;

    @Column(nullable = false)
    private String run;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String professionalTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcademicDegree academicDegree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;
}
