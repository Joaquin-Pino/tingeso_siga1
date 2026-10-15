package cl.joaquin.siga.Repositories.People;

import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.University.CourseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    boolean existsByStudentId(Long studentId);
    List<AcademicRecord> findByStudentId(Long studentId);
    List<AcademicRecord> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
    List<AcademicRecord> findByStudentIdAndResult(Long studentId, CourseResult result);
    boolean existsBySubjectId(Long subjectId);
    List<AcademicRecord> findByAcademicPeriodId(Long academicPeriodId);
}
