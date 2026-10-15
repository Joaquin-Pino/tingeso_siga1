package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.SectionScheduleBlock;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.SubjectPrerequisite;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SectionScheduleBlockRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock CourseRegistrationRepository courseRegistrationRepository;
    @Mock StudentRepository studentRepository;
    @Mock SectionRepository sectionRepository;
    @Mock SectionScheduleBlockRepository sectionScheduleBlockRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    @Mock AcademicRecordRepository academicRecordRepository;
    @Mock AcademicPeriodRepository academicPeriodRepository;
    @Mock PeriodEnrollmentRepository periodEnrollmentRepository;

    CourseRegistrationService service;

    private static final Long STUDENT_ID = 3L;
    private static final Long PLAN_ID = 7L;
    private static final Long PERIOD_ID = 10L;

    private Student student;
    private AcademicPeriod period;
    private final List<AcademicRecord> approved = new ArrayList<>();
    private final List<CourseRegistration> existing = new ArrayList<>();

    // plan: asignaturas 1-6 de semestre 1, 7 (prerrequisito 1) y 8 (sin prerrequisitos) de semestre 2.
    // la sección de la asignatura n tiene id 100 + n y todas se ofrecen en el período
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2027-03-01T12:00:00Z"), ZoneId.of("America/Santiago"));
        service = new CourseRegistrationService(courseRegistrationRepository, studentRepository, sectionRepository,
                sectionScheduleBlockRepository, subjectRepository, subjectPrerequisiteRepository,
                academicRecordRepository, academicPeriodRepository, periodEnrollmentRepository, clock);

        student = new Student();
        student.setId(STUDENT_ID);
        student.setStudyPlanId(PLAN_ID);
        student.setStatus(StudentStatus.REGULAR);
        period = new AcademicPeriod();
        period.setId(PERIOD_ID);
        period.setStatus(PeriodStatus.OPEN);

        List<Subject> subjects = new ArrayList<>();
        for (long id = 1; id <= 8; id++) {
            subjects.add(subject(id, id >= 7 ? 2 : 1));
        }
        List<Section> offered = new ArrayList<>();
        for (long id = 1; id <= 8; id++) {
            offered.add(section(id));
        }
        SubjectPrerequisite prerequisite = new SubjectPrerequisite();
        prerequisite.setSubjectId(7L);
        prerequisite.setPrerequisiteSubjectId(1L);

        lenient().when(studentRepository.findById(STUDENT_ID)).thenAnswer(i -> Optional.of(student));
        lenient().when(academicPeriodRepository.findById(PERIOD_ID)).thenAnswer(i -> Optional.of(period));
        lenient().when(periodEnrollmentRepository.existsByStudentIdAndAcademicPeriodId(STUDENT_ID, PERIOD_ID))
                .thenReturn(true);
        lenient().when(subjectRepository.findByStudyPlanId(PLAN_ID)).thenReturn(subjects);
        lenient().when(subjectPrerequisiteRepository.findBySubjectIdIn(anyCollection()))
                .thenReturn(List.of(prerequisite));
        lenient().when(academicRecordRepository.findByStudentIdAndResult(STUDENT_ID, CourseResult.PASSED))
                .thenReturn(approved);
        lenient().when(courseRegistrationRepository.findByStudentIdAndAcademicPeriodId(STUDENT_ID, PERIOD_ID))
                .thenReturn(existing);
        lenient().when(sectionRepository.findByAcademicPeriodId(PERIOD_ID)).thenReturn(offered);
        lenient().when(sectionRepository.reserveSeat(any())).thenReturn(1);
        lenient().when(sectionScheduleBlockRepository.findBySectionIdIn(anyCollection())).thenAnswer(i -> {
            // un bloque propio por sección (módulo = n), salvo que el test pida otra cosa
            List<SectionScheduleBlock> blocks = new ArrayList<>();
            for (Long sectionId : i.<java.util.Collection<Long>>getArgument(0)) {
                blocks.add(block(sectionId, WeekDay.L, (int) (sectionId - 100)));
            }
            return blocks;
        });
        lenient().when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> {
            CourseRegistration r = i.getArgument(0);
            r.setId(r.getSectionId() + 1000);
            return r;
        });
    }

    private static Subject subject(Long id, int semester) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setCode("S" + id);
        subject.setSemester(semester);
        return subject;
    }

    private static Section section(Long subjectId) {
        Section section = new Section();
        section.setId(100 + subjectId);
        section.setSubjectId(subjectId);
        section.setAcademicPeriodId(PERIOD_ID);
        return section;
    }

    private static SectionScheduleBlock block(Long sectionId, WeekDay day, int module) {
        SectionScheduleBlock block = new SectionScheduleBlock();
        block.setSectionId(sectionId);
        block.setDay(day);
        block.setModule(module);
        return block;
    }

    private void approve(Long... subjectIds) {
        for (Long id : subjectIds) {
            AcademicRecord record = new AcademicRecord();
            record.setSubjectId(id);
            approved.add(record);
        }
    }

    private void alreadyRegistered(Long... subjectIds) {
        for (Long id : subjectIds) {
            CourseRegistration registration = new CourseRegistration();
            registration.setId(id + 500);
            registration.setStudentId(STUDENT_ID);
            registration.setSubjectId(id);
            registration.setSectionId(100 + id);
            registration.setAcademicPeriodId(PERIOD_ID);
            existing.add(registration);
        }
    }

    // arma el lote por asignatura (sección = 100 + n) y deja findAllById respondiendo
    private List<Long> batch(Long... subjectIds) {
        List<Long> sectionIds = new ArrayList<>();
        List<Section> sections = new ArrayList<>();
        for (Long id : subjectIds) {
            sectionIds.add(100 + id);
            sections.add(section(id));
        }
        lenient().when(sectionRepository.findAllById(sectionIds)).thenReturn(sections);
        return sectionIds;
    }

    private IllegalStateException assertRejected(List<Long> sectionIds) {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.register(STUDENT_ID, sectionIds));
        verify(sectionRepository, never()).reserveSeat(any());
        verify(courseRegistrationRepository, never()).save(any());
        return ex;
    }

    // ---- inscribir ----

    @Test
    void register_validBatch_reservesSeatsAndSavesRegistrations() {
        List<CourseRegistrationResponseDTO> result = service.register(STUDENT_ID, batch(1L, 2L, 3L));

        assertEquals(3, result.size());
        assertEquals(PERIOD_ID, result.get(0).academicPeriodId());
        assertEquals(LocalDate.of(2027, 3, 1), result.get(0).registrationDate());
        assertNull(result.get(0).grade());
        verify(sectionRepository).reserveSeat(101L);
        verify(sectionRepository).reserveSeat(102L);
        verify(sectionRepository).reserveSeat(103L);
    }

    @Test
    void register_reservesSeatsInIdOrder() {
        service.register(STUDENT_ID, batch(3L, 1L, 2L));

        var order = inOrder(sectionRepository);
        order.verify(sectionRepository).reserveSeat(101L);
        order.verify(sectionRepository).reserveSeat(102L);
        order.verify(sectionRepository).reserveSeat(103L);
    }

    @Test
    void register_secondBatchCountsAlreadyRegisteredSubjects() {
        alreadyRegistered(1L, 2L, 3L);

        assertDoesNotThrow(() -> service.register(STUDENT_ID, batch(4L)));
    }

    @Test
    void register_studentNotFound_throwsNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.register(99L, List.of(101L)));
    }

    @Test
    void register_studentNotRegular_isRejected() {
        student.setStatus(StudentStatus.POSTPONED);

        assertRejected(batch(1L, 2L, 3L));
    }

    @Test
    void register_notEnrolledInPeriod_isRejected() {
        when(periodEnrollmentRepository.existsByStudentIdAndAcademicPeriodId(STUDENT_ID, PERIOD_ID)).thenReturn(false);

        assertRejected(batch(1L, 2L, 3L));
    }

    @Test
    void register_closedPeriod_isRejected() {
        period.setStatus(PeriodStatus.CLOSED);

        assertRejected(batch(1L, 2L, 3L));
    }

    @Test
    void register_repeatedSectionIds_isBadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, List.of(101L, 101L, 102L)));
    }

    @Test
    void register_unknownSection_throwsNotFound() {
        when(sectionRepository.findAllById(List.of(101L, 999L))).thenReturn(List.of(section(1L)));

        assertThrows(NotFoundException.class, () -> service.register(STUDENT_ID, List.of(101L, 999L)));
    }

    @Test
    void register_sectionsFromDifferentPeriods_isBadRequest() {
        Section other = section(2L);
        other.setAcademicPeriodId(11L);
        when(sectionRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(section(1L), other));

        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, List.of(101L, 102L)));
    }

    @Test
    void register_subjectOutsideStudentPlan_isRejected() {
        Section foreign = section(99L);
        when(sectionRepository.findAllById(List.of(199L))).thenReturn(List.of(foreign));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.register(STUDENT_ID, List.of(199L)));
        assertTrue(ex.getMessage().contains("plan"));
    }

    @Test
    void register_approvedSubject_isRejected() {
        approve(1L);

        assertTrue(assertRejected(batch(1L, 2L, 3L)).getMessage().contains("aprobada"));
    }

    @Test
    void register_previouslyFailedSubject_canBeRegisteredAgain() {
        // una reprobada no figura entre las aprobadas (el historial se filtra por PASSED)
        assertDoesNotThrow(() -> service.register(STUDENT_ID, batch(1L, 2L, 3L)));
    }

    @Test
    void register_subjectAlreadyRegisteredInPeriod_isRejected() {
        alreadyRegistered(1L);

        assertTrue(assertRejected(batch(1L, 2L, 3L)).getMessage().contains("ya está inscrita"));
    }

    @Test
    void register_unapprovedPrerequisite_isRejected() {
        assertTrue(assertRejected(batch(7L, 2L, 3L)).getMessage().contains("prerrequisitos"));
    }

    @Test
    void register_approvedPrerequisite_isAccepted() {
        approve(1L, 2L, 3L, 4L, 5L, 6L); // quedan 7 y 8, y la 7 ya tiene su prerrequisito

        assertDoesNotThrow(() -> service.register(STUDENT_ID, batch(7L, 8L)));
    }

    @Test
    void register_fewerThanThree_isRejectedWhileMoreAreAvailable() {
        assertTrue(assertRejected(batch(1L, 2L)).getMessage().contains("al menos 3"));
    }

    @Test
    void register_moreThanSix_isRejected() {
        alreadyRegistered(1L, 2L, 3L, 4L);

        assertTrue(assertRejected(batch(5L, 6L, 8L)).getMessage().contains("más de 6"));
    }

    @Test
    void register_exactlySix_isAccepted() {
        assertDoesNotThrow(() -> service.register(STUDENT_ID, batch(1L, 2L, 3L, 4L, 5L, 6L)));
    }

    @Test
    void register_fewerThanThreeLeftInPlan_allowsRegisteringTheRemaining() {
        approve(1L, 2L, 3L, 4L, 5L, 6L, 7L); // solo queda la 8

        assertDoesNotThrow(() -> service.register(STUDENT_ID, batch(8L)));
    }

    @Test
    void register_fewerThanThreeLeftInPlan_stillRequiresAllOfThem() {
        approve(1L, 2L, 3L, 4L, 5L, 6L); // quedan 7 y 8

        assertTrue(assertRejected(batch(7L)).getMessage().contains("al menos 2"));
    }

    @Test
    void register_higherSemesterWhileLowerPending_isRejected() {
        approve(1L); // 7 y 8 son de semestre 2; quedan 2-6 pendientes de semestre 1

        assertTrue(assertRejected(batch(7L, 8L, 2L)).getMessage().contains("inferior"));
    }

    @Test
    void register_higherSemesterWhenLowerAreAllRegistered_isAccepted() {
        approve(1L);
        alreadyRegistered(2L, 3L, 4L, 5L, 6L);

        assertDoesNotThrow(() -> service.register(STUDENT_ID, batch(7L)));
    }

    @Test
    void register_lowerSemesterPendingButUnavailable_doesNotBlock() {
        // la 7 es de semestre 2 y la 8 también: ninguna bloquea a la otra, y sin prerrequisito aprobado la 7 no cuenta
        approve(2L, 3L, 4L, 5L, 6L); // la 1 sigue pendiente (semestre 1), pero 7 y 8 piden el lote completo

        assertTrue(assertRejected(batch(8L, 7L, 2L)).getMessage().contains("prerrequisitos"));
    }

    @Test
    void register_scheduleConflictInsideBatch_isRejected() {
        when(sectionScheduleBlockRepository.findBySectionIdIn(anyCollection())).thenReturn(List.of(
                block(101L, WeekDay.L, 1), block(102L, WeekDay.L, 1), block(103L, WeekDay.M, 3)));

        assertTrue(assertRejected(batch(1L, 2L, 3L)).getMessage().contains("Conflicto de horario"));
    }

    @Test
    void register_scheduleConflictWithExistingRegistration_isRejected() {
        alreadyRegistered(1L);
        when(sectionScheduleBlockRepository.findBySectionIdIn(anyCollection())).thenReturn(List.of(
                block(101L, WeekDay.L, 1), block(102L, WeekDay.L, 1), block(103L, WeekDay.M, 3),
                block(104L, WeekDay.W, 3)));

        assertTrue(assertRejected(batch(2L, 3L, 4L)).getMessage().contains("Conflicto de horario"));
    }

    @Test
    void register_sectionWithoutSeat_isRejected() {
        when(sectionRepository.reserveSeat(102L)).thenReturn(0);

        List<Long> ids = batch(1L, 2L, 3L);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.register(STUDENT_ID, ids));

        assertTrue(ex.getMessage().contains("cupo"));
        verify(courseRegistrationRepository, never()).save(any());
    }

    // ---- anular ----

    private CourseRegistration registration(Long id, Long studentId) {
        CourseRegistration registration = new CourseRegistration();
        registration.setId(id);
        registration.setStudentId(studentId);
        registration.setSectionId(101L);
        registration.setAcademicPeriodId(PERIOD_ID);
        return registration;
    }

    @Test
    void cancel_leavingThreeOrMore_deletesRowAndReleasesSeat() {
        CourseRegistration registration = registration(1L, STUDENT_ID);
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        alreadyRegistered(1L, 2L, 3L, 4L);
        when(sectionRepository.releaseSeat(101L)).thenReturn(1);

        service.cancel(STUDENT_ID, 1L);

        verify(courseRegistrationRepository).delete(registration);
        verify(sectionRepository).releaseSeat(101L);
    }

    @Test
    void cancel_leavingFewerThanThree_isRejected() {
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration(1L, STUDENT_ID)));
        alreadyRegistered(1L, 2L, 3L);

        assertThrows(IllegalStateException.class, () -> service.cancel(STUDENT_ID, 1L));
        verify(courseRegistrationRepository, never()).delete(any());
        verify(sectionRepository, never()).releaseSeat(any());
    }

    @Test
    void cancel_registrationOfAnotherStudent_throwsNotFound() {
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration(1L, 99L)));

        assertThrows(NotFoundException.class, () -> service.cancel(STUDENT_ID, 1L));
    }

    @Test
    void cancel_closedPeriod_isRejected() {
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration(1L, STUDENT_ID)));
        period.setStatus(PeriodStatus.CLOSED);

        assertThrows(IllegalStateException.class, () -> service.cancel(STUDENT_ID, 1L));
        verify(courseRegistrationRepository, never()).delete(any());
    }

    @Test
    void cancel_counterAlreadyZero_failsSoTheTransactionRollsBack() {
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration(1L, STUDENT_ID)));
        alreadyRegistered(1L, 2L, 3L, 4L);
        when(sectionRepository.releaseSeat(101L)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.cancel(STUDENT_ID, 1L));
    }

    // ---- consultar ----

    @Test
    void getByStudent_withoutPeriod_returnsAll() {
        when(courseRegistrationRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(registration(1L, STUDENT_ID)));

        assertEquals(1, service.getByStudent(STUDENT_ID, null).size());
    }

    @Test
    void getByStudent_withPeriod_filtersByPeriod() {
        alreadyRegistered(1L, 2L);

        assertEquals(2, service.getByStudent(STUDENT_ID, PERIOD_ID).size());
        verify(courseRegistrationRepository, never()).findByStudentId(any());
    }

    @Test
    void cancel_registrationWithGrade_isRejected() {
        CourseRegistration registration = registration(1L, STUDENT_ID);
        registration.setGrade(new java.math.BigDecimal("5.0"));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(IllegalStateException.class, () -> service.cancel(STUDENT_ID, 1L));
        verify(courseRegistrationRepository, never()).delete(any());
        verify(sectionRepository, never()).releaseSeat(any());
    }
}
