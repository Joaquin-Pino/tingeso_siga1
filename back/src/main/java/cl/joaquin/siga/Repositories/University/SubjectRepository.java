package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByStudyPlanId(Long studyPlanId);

    boolean existsByStudyPlanIdAndCode(Long studyPlanId, String code);

    boolean existsByStudyPlanId(Long studyPlanId);
}
