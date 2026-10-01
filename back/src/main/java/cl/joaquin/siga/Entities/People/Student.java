package cl.joaquin.siga.Entities.People;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
// uk -> unique key, sin esto se genera un hash feo
@Table(
        name = "student",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_run", columnNames = "run"),
                @UniqueConstraint(name = "uk_student_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_student_keycloak_id", columnNames = "keycloak_id")
        }
)
public class Student {
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
    private Long careerId;

    @Column(nullable = false)
    private Long studyPlanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;
}