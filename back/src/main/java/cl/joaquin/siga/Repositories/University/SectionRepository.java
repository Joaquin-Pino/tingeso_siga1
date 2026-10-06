package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.Section;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {
    boolean existsBySubjectId(Long subjectId);
}
