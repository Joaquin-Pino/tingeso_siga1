package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.PeriodEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodEnrollmentRepository extends JpaRepository<PeriodEnrollment, Long> {
    boolean existsByAcademicPeriodId(Long academicPeriodId);
}
