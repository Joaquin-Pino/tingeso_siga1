package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.CourseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {
    boolean existsByStudentId(Long studentId);

    boolean existsBySubjectId(Long subjectId);

    boolean existsBySectionId(Long sectionId);

    List<CourseRegistration> findByStudentId(Long studentId);

    List<CourseRegistration> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
}
