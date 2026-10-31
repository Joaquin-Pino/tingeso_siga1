package cl.joaquin.siga.Repositories.People;

import cl.joaquin.siga.Entities.People.TeachingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeachingRecordRepository extends JpaRepository<TeachingRecord, Long> {
    List<TeachingRecord> findByTeacherId(Long teacherId);
    List<TeachingRecord> findByAcademicPeriodId(Long academicPeriodId);
}
