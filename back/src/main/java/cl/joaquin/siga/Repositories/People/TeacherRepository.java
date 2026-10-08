package cl.joaquin.siga.Repositories.People;

import cl.joaquin.siga.Entities.People.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    boolean existsByRun(String run);
    boolean existsByEmail(String email);
}
