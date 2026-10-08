package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    boolean existsBySubjectId(Long subjectId);
    boolean existsByTeacherId(Long teacherId);
    List<Section> findByTeacherId(Long teacherId);
}
