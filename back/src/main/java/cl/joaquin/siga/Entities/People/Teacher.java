package cl.joaquin.siga.Entities.People;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", nullable = false)
    private UUID keyCloakId;

    private String run;
    private String email;

    private String professionalTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcademicDegree academicDegree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;
}
