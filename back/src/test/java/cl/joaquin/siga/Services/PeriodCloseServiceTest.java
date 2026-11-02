package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.PeriodCloseResponseDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeachingRecordRepository;
import cl.joaquin.siga.Entities.People.TeachingRecord;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodCloseServiceTest {

    private static final Long PERIOD_ID = 10L;
    private static final Long STUDENT_ID = 3L;
    private static final Long PLAN_ID = 7L;

    @Mock AcademicPeriodRepository academicPeriodRepository;
    @Mock CourseRegistrationRepository courseRegistrationRepository;
    @Mock AcademicRecordRepository academicRecordRepository;
    @Mock SectionRepository sectionRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock StudentRepository studentRepository;
    @Mock TeachingRecordRepository teachingRecordRepository;
    @InjectMocks PeriodCloseService service;

    private AcademicPeriod period;
    private Student student;
    // lo que "ya está en la BD" como historial del estudiante (los registros que crea el cierre se agregan aquí)
    private final List<AcademicRecord> history = new ArrayList<>();
    private final List<AcademicRecord> periodRecords = new ArrayList<>();

    @BeforeEach
    void setUp() {
        period = new AcademicPeriod();
        period.setId(PERIOD_ID);
        period.setYear(2027);
        period.setSemester(Semester.FIRST);
        period.setStatus(PeriodStatus.OPEN);
        student = new Student();
        student.setId(STUDENT_ID);
        student.setStudyPlanId(PLAN_ID);
        student.setStatus(StudentStatus.REGULAR);

        lenient().when(academicPeriodRepository.findById(PERIOD_ID)).thenAnswer(i -> Optional.of(period));
        lenient().when(studentRepository.findById(STUDENT_ID)).thenAnswer(i -> Optional.of(student));
        lenient().when(subjectRepository.findAllById(any())).thenAnswer(i -> {
            List<Subject> found = new ArrayList<>();
            for (Long id : i.<Iterable<Long>>getArgument(0)) {
                found.add(subject(id));
            }
            return found;
        });
        lenient().when(subjectRepository.findByStudyPlanId(PLAN_ID)).thenReturn(List.of(subject(1L), subject(2L)));
        lenient().when(academicRecordRepository.findByAcademicPeriodId(PERIOD_ID)).thenReturn(periodRecords);
        lenient().when(academicRecordRepository.findByStudentId(STUDENT_ID)).thenReturn(history);
        lenient().when(academicRecordRepository.save(any(AcademicRecord.class))).thenAnswer(i -> {
            AcademicRecord record = i.getArgument(0);
            history.add(record);
            return record;
        });
    }

    private static Subject subject(Long id) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setCode("S" + id);
        subject.setName("Asignatura " + id);
        subject.setSemester(1);
        subject.setCredits(5);
        return subject;
    }

    private static CourseRegistration registration(Long id, Long subjectId, String grade) {
        CourseRegistration registration = new CourseRegistration();
        registration.setId(id);
        registration.setStudentId(STUDENT_ID);
        registration.setSectionId(100 + subjectId);
        registration.setSubjectId(subjectId);
        registration.setAcademicPeriodId(PERIOD_ID);
        registration.setGrade(grade == null ? null : new BigDecimal(grade));
        return registration;
    }

    private static AcademicRecord oldRecord(Long subjectId, CourseResult result) {
        AcademicRecord record = new AcademicRecord();
        record.setStudentId(STUDENT_ID);
        record.setSubjectId(subjectId);
        record.setResult(result);
        return record;
    }

    private void registrations(CourseRegistration... rows) {
        when(courseRegistrationRepository.findByAcademicPeriodId(PERIOD_ID)).thenReturn(List.of(rows));
    }

    @Test
    void close_computesResultsCreatesSnapshotsAndClosesPeriod() {
        CourseRegistration passed = registration(1L, 1L, "4.0");
        CourseRegistration failed = registration(2L, 2L, "3.9");
        registrations(passed, failed);

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertEquals(CourseResult.PASSED, passed.getResult());
        assertEquals(CourseResult.FAILED, failed.getResult());
        assertEquals(2, response.recordsCreated());
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
        assertEquals(PeriodStatus.CLOSED, response.period().status());

        ArgumentCaptor<AcademicRecord> captor = ArgumentCaptor.forClass(AcademicRecord.class);
        verify(academicRecordRepository, times(2)).save(captor.capture());
        AcademicRecord snapshot = captor.getAllValues().get(0);
        assertEquals(1L, snapshot.getCourseRegistrationId());
        assertEquals("S1", snapshot.getSubjectCode());
        assertEquals("Asignatura 1", snapshot.getSubjectName());
        assertEquals(5, snapshot.getCredits());
        assertEquals("2027-1", snapshot.getPeriodCode());
        assertEquals(new BigDecimal("4.0"), snapshot.getFinalGrade());
        assertEquals(CourseResult.PASSED, snapshot.getResult());
        verify(academicPeriodRepository).save(period);
    }

    @Test
    void close_snapshotsEverySectionIntoTeachingHistory() {
        registrations(registration(1L, 1L, "5.0"));
        Section section = new Section();
        section.setId(101L);
        section.setSubjectId(1L);
        section.setTeacherId(8L);
        section.setEnrolledCount(12);
        when(sectionRepository.findByAcademicPeriodId(PERIOD_ID)).thenReturn(List.of(section));

        service.closePeriod(PERIOD_ID);

        ArgumentCaptor<TeachingRecord> captor = ArgumentCaptor.forClass(TeachingRecord.class);
        verify(teachingRecordRepository).save(captor.capture());
        TeachingRecord record = captor.getValue();
        assertEquals(8L, record.getTeacherId());
        assertEquals(101L, record.getSectionId());
        assertEquals("S1", record.getSubjectCode());
        assertEquals("2027-1", record.getPeriodCode());
        assertEquals(12, record.getEnrolledCount());
    }

    @Test
    void close_teachingHistoryIsIdempotent() {
        registrations(registration(1L, 1L, "5.0"));
        Section section = new Section();
        section.setId(101L);
        section.setSubjectId(1L);
        when(sectionRepository.findByAcademicPeriodId(PERIOD_ID)).thenReturn(List.of(section));
        TeachingRecord existing = new TeachingRecord();
        existing.setSectionId(101L);
        when(teachingRecordRepository.findByAcademicPeriodId(PERIOD_ID)).thenReturn(List.of(existing));

        service.closePeriod(PERIOD_ID);

        verify(teachingRecordRepository, never()).save(any());
    }

    @Test
    void close_isIdempotentOnRecords_doesNotDuplicateExistingSnapshots() {
        registrations(registration(1L, 1L, "5.0"), registration(2L, 2L, "5.0"));
        AcademicRecord existing = new AcademicRecord();
        existing.setCourseRegistrationId(1L);
        periodRecords.add(existing);

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertEquals(1, response.recordsCreated());
        verify(academicRecordRepository, times(1)).save(any(AcademicRecord.class));
    }

    @Test
    void close_missingGrades_isRejectedAndListsSections() {
        registrations(registration(1L, 1L, "5.0"), registration(2L, 2L, null), registration(3L, 2L, null));
        Section section = new Section();
        section.setId(102L);
        section.setSubjectId(2L);
        when(sectionRepository.findAllById(any())).thenReturn(List.of(section));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.closePeriod(PERIOD_ID));

        assertTrue(ex.getMessage().contains("sección 102"));
        assertTrue(ex.getMessage().contains("S2"));
        assertTrue(ex.getMessage().contains("2 sin nota"));
        assertEquals(PeriodStatus.OPEN, period.getStatus());
        verify(academicRecordRepository, never()).save(any());
        verify(academicPeriodRepository, never()).save(any());
    }

    @Test
    void close_alreadyClosed_isRejected() {
        period.setStatus(PeriodStatus.CLOSED);

        assertThrows(IllegalStateException.class, () -> service.closePeriod(PERIOD_ID));
        verify(courseRegistrationRepository, never()).findByAcademicPeriodId(any());
    }

    @Test
    void close_periodNotFound_throws() {
        assertThrows(NotFoundException.class, () -> service.closePeriod(99L));
    }

    @Test
    void close_secondFailOfSameSubject_eliminatesStudent() {
        history.add(oldRecord(1L, CourseResult.FAILED));
        registrations(registration(1L, 1L, "2.0"), registration(2L, 2L, "6.0"));

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertEquals(StudentStatus.ELIMINATED, student.getStatus());
        assertEquals(List.of(STUDENT_ID), response.eliminatedStudentIds());
        assertTrue(response.graduatedStudentIds().isEmpty());
        verify(studentRepository).save(student);
    }

    @Test
    void close_firstFail_doesNotEliminate() {
        registrations(registration(1L, 1L, "2.0"), registration(2L, 2L, "6.0"));

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertEquals(StudentStatus.REGULAR, student.getStatus());
        assertTrue(response.eliminatedStudentIds().isEmpty());
        assertTrue(response.graduatedStudentIds().isEmpty());
    }

    @Test
    void close_failsDifferentSubjectsOnce_doesNotEliminate() {
        history.add(oldRecord(2L, CourseResult.FAILED));
        registrations(registration(1L, 1L, "2.0"));

        service.closePeriod(PERIOD_ID);

        assertEquals(StudentStatus.REGULAR, student.getStatus());
    }

    @Test
    void close_approvedWholePlan_graduatesStudent() {
        history.add(oldRecord(1L, CourseResult.PASSED));
        registrations(registration(2L, 2L, "5.0"));

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertEquals(StudentStatus.GRADUATED, student.getStatus());
        assertEquals(List.of(STUDENT_ID), response.graduatedStudentIds());
        verify(studentRepository).save(student);
    }

    @Test
    void close_planSubjectStillPending_doesNotGraduate() {
        registrations(registration(1L, 1L, "5.0"));

        service.closePeriod(PERIOD_ID);

        assertEquals(StudentStatus.REGULAR, student.getStatus());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void close_studentAlreadyGraduated_isSkipped() {
        student.setStatus(StudentStatus.GRADUATED);
        registrations(registration(1L, 1L, "5.0"));

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertTrue(response.graduatedStudentIds().isEmpty());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void close_periodWithoutRegistrations_justCloses() {
        registrations();

        PeriodCloseResponseDTO response = service.closePeriod(PERIOD_ID);

        assertEquals(0, response.recordsCreated());
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
    }
}
