package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByCareerIdAndStatus(Long careerId, StudyPlanStatus status);
}
