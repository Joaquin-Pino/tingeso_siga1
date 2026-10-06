package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.SubjectPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectPrerequisiteRepository extends JpaRepository<SubjectPrerequisite, Long> {
    List<SubjectPrerequisite> findBySubjectId(Long subjectId);

    // asignaturas que tienen a prerequisiteSubjectId como prerrequisito
    List<SubjectPrerequisite> findByPrerequisiteSubjectId(Long prerequisiteSubjectId);

    boolean existsByPrerequisiteSubjectId(Long prerequisiteSubjectId);

    void deleteBySubjectId(Long subjectId);
}
