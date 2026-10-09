package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.SectionScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionScheduleBlockRepository extends JpaRepository<SectionScheduleBlock, Long> {
    List<SectionScheduleBlock> findBySectionId(Long sectionId);

    void deleteBySectionId(Long sectionId);
}
