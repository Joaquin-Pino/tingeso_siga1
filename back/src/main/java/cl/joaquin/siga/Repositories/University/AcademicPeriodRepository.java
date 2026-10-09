package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {
    boolean existsByIdAndStatus(Long id, PeriodStatus status);

    boolean existsByYearAndSemester(Integer year, Semester semester);
}
