package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.CourseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {
    boolean existsByStudentId(Long studentId);
}
