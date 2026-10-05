package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByCareerId(Long careerId);

    boolean existsByCareerIdAndCode(Long careerId, String code);

    boolean existsByCareerId(Long careerId);

    Optional<StudyPlan> findByCareerIdAndStatus(Long careerId, StudyPlanStatus status);

}
