package cl.joaquin.siga.Repositories.People;

import cl.joaquin.siga.Entities.People.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRun(String run);
    boolean existsByEmail(String email);
    Optional<Student> findByKeycloakId(String keycloakId);
}
