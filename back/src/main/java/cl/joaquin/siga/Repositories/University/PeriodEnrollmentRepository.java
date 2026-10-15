package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.PeriodEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodEnrollmentRepository extends JpaRepository<PeriodEnrollment, Long> {
    boolean existsByAcademicPeriodId(Long academicPeriodId);
    boolean existsByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
    List<PeriodEnrollment> findByAcademicPeriodId(Long academicPeriodId);
}
