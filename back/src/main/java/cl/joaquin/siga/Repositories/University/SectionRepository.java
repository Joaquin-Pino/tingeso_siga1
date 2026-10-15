package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    boolean existsBySubjectId(Long subjectId);
    boolean existsByTeacherId(Long teacherId);
    boolean existsByAcademicPeriodId(Long academicPeriodId);
    boolean existsBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);
    List<Section> findByTeacherId(Long teacherId);
    List<Section> findByAcademicPeriodId(Long academicPeriodId);
    List<Section> findByTeacherIdAndAcademicPeriodId(Long teacherId, Long academicPeriodId);

    // toma un cupo de forma atómica: la condición va en el UPDATE, así que dos réplicas no pueden pasar el tope.
    // devuelve las filas afectadas (0 = sin cupo)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Section s set s.enrolledCount = s.enrolledCount + 1, s.version = s.version + 1 "
            + "where s.id = :id and s.enrolledCount < s.capacity")
    int reserveSeat(@Param("id") Long id);

    // libera un cupo (anulación); devuelve las filas afectadas (0 = el contador ya estaba en 0)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Section s set s.enrolledCount = s.enrolledCount - 1, s.version = s.version + 1 "
            + "where s.id = :id and s.enrolledCount > 0")
    int releaseSeat(@Param("id") Long id);
}
