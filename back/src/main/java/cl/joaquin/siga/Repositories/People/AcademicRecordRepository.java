package cl.joaquin.siga.Repositories.People;

import cl.joaquin.siga.Entities.People.AcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    boolean existsByStudentId(Long studentId);
    List<AcademicRecord> findByStudentId(Long studentId);
}
