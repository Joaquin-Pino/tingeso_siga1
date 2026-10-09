package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionUpdateDTO;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Entities.People.TeacherStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.SectionScheduleBlock;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Entities.University.WeeklyHours;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SectionScheduleBlockRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock SectionRepository sectionRepository;
    @Mock SectionScheduleBlockRepository sectionScheduleBlockRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock AcademicPeriodRepository academicPeriodRepository;
    @Mock CourseRegistrationRepository courseRegistrationRepository;
    @InjectMocks SectionService service;

    // asignatura 2-2-2: 6 horas, requiere 3 bloques
    private static final List<ScheduleBlockDTO> THREE_BLOCKS = List.of(
            new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.M, 1), new ScheduleBlockDTO(WeekDay.W, 1));

    private static Subject subject(int theory, int exercise, int lab) {
        Subject subject = new Subject();
        subject.setId(5L);
        subject.setCode("TAP101");
        subject.setWeeklyHours(new WeeklyHours(theory, exercise, lab));
        return subject;
    }

    private static AcademicPeriod period(PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(10L);
        period.setStatus(status);
        return period;
    }

    private static Teacher teacher(Long id, TeacherStatus status) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setStatus(status);
        return teacher;
    }

    private static Section section(Long id, Long teacherId, int capacity, int enrolledCount) {
        Section section = new Section();
        section.setId(id);
        section.setSubjectId(5L);
        section.setAcademicPeriodId(10L);
        section.setTeacherId(teacherId);
        section.setCapacity(capacity);
        section.setEnrolledCount(enrolledCount);
        return section;
    }

    private static SectionScheduleBlock block(Long sectionId, WeekDay day, int module) {
        SectionScheduleBlock block = new SectionScheduleBlock();
        block.setSectionId(sectionId);
        block.setDay(day);
        block.setModule(module);
        return block;
    }

    private static List<SectionScheduleBlock> threeBlockRows(Long sectionId) {
        return List.of(block(sectionId, WeekDay.L, 1), block(sectionId, WeekDay.M, 1), block(sectionId, WeekDay.W, 1));
    }

    private static SectionCreateDTO createDTO(List<ScheduleBlockDTO> blocks) {
        return new SectionCreateDTO(5L, 10L, 7L, 30, blocks);
    }

    // deja pasar todas las validaciones previas a los bloques en createSection
    private void stubValidCreate(Subject subject) {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findById(7L)).thenReturn(Optional.of(teacher(7L, TeacherStatus.ACTIVE)));
    }

    // ---- consultas ----

    @Test
    void getById_returnsSectionWithBlocks() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section(1L, 7L, 30, 4)));
        when(sectionScheduleBlockRepository.findBySectionId(1L)).thenReturn(threeBlockRows(1L));

        SectionResponseDTO result = service.getById(1L);

        assertEquals(4, result.enrolledCount());
        assertEquals(THREE_BLOCKS, result.scheduleBlocks());
    }

    @Test
    void getById_notFound_throws() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void getAllPeriodSections_mapsSections() {
        when(academicPeriodRepository.existsById(10L)).thenReturn(true);
        when(sectionRepository.findByAcademicPeriodId(10L))
                .thenReturn(List.of(section(1L, 7L, 30, 0), section(2L, 8L, 30, 0)));

        assertEquals(2, service.getAllPeriodSections(10L).size());
    }

    @Test
    void getAllPeriodSections_periodNotFound_throws() {
        when(academicPeriodRepository.existsById(10L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getAllPeriodSections(10L));
    }

    @Test
    void getAllTeacherSections_mapsSections() {
        when(teacherRepository.existsById(7L)).thenReturn(true);
        when(sectionRepository.findByTeacherId(7L)).thenReturn(List.of(section(1L, 7L, 30, 0)));

        assertEquals(1, service.getAllTeacherSections(7L).size());
    }

    @Test
    void getAllTeacherSections_teacherNotFound_throws() {
        when(teacherRepository.existsById(7L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getAllTeacherSections(7L));
    }

    // ---- crear ----

    @Test
    void createSection_valid_savesSectionAndBlocks() {
        stubValidCreate(subject(2, 2, 2));
        when(sectionRepository.save(any(Section.class))).thenAnswer(i -> {
            Section s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        SectionResponseDTO result = service.createSection(createDTO(THREE_BLOCKS));

        assertEquals(0, result.enrolledCount());
        assertEquals(30, result.capacity());
        assertEquals(THREE_BLOCKS, result.scheduleBlocks());
        verify(sectionScheduleBlockRepository, times(3)).save(any(SectionScheduleBlock.class));
    }

    @Test
    void createSection_periodNotFound_throws() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_closedPeriod_throws() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.CLOSED)));

        assertThrows(IllegalStateException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_subjectNotFound_throws() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(subjectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
    }

    @Test
    void createSection_subjectAlreadyOfferedInPeriod_throws() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(5L, 10L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_teacherNotFound_throws() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(teacherRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
    }

    @Test
    void createSection_inactiveTeacher_throws() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(teacherRepository.findById(7L)).thenReturn(Optional.of(teacher(7L, TeacherStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_duplicateBlocks_throws() {
        stubValidCreate(subject(2, 2, 2));
        List<ScheduleBlockDTO> blocks = List.of(
                new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.W, 1));

        assertThrows(IllegalArgumentException.class, () -> service.createSection(createDTO(blocks)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_wrongBlockCount_throws() {
        stubValidCreate(subject(2, 2, 0)); // 4 horas: requiere 2 bloques, no 3

        assertThrows(IllegalArgumentException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_oddWeeklyHours_throws() {
        stubValidCreate(subject(2, 2, 1));

        assertThrows(IllegalArgumentException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_teacherScheduleConflict_throws() {
        stubValidCreate(subject(2, 2, 2));
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(7L, 10L)).thenReturn(List.of(section(2L, 7L, 30, 0)));
        when(sectionScheduleBlockRepository.findBySectionId(2L))
                .thenReturn(List.of(block(2L, WeekDay.V, 3), block(2L, WeekDay.M, 1)));

        assertThrows(IllegalStateException.class, () -> service.createSection(createDTO(THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_teacherOtherSectionWithoutOverlap_ok() {
        stubValidCreate(subject(2, 2, 2));
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(7L, 10L)).thenReturn(List.of(section(2L, 7L, 30, 0)));
        when(sectionScheduleBlockRepository.findBySectionId(2L)).thenReturn(List.of(block(2L, WeekDay.L, 2)));
        when(sectionRepository.save(any(Section.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.createSection(createDTO(THREE_BLOCKS)));
    }

    // ---- editar ----

    private void stubUpdatableSection(Section section) {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
    }

    @Test
    void updateSection_sameBlocks_updatesCapacityWithoutTouchingBlocks() {
        stubUpdatableSection(section(1L, 7L, 30, 10));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(sectionScheduleBlockRepository.findBySectionId(1L)).thenReturn(threeBlockRows(1L));
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(7L, 10L)).thenReturn(List.of(section(1L, 7L, 30, 10)));
        when(sectionRepository.save(any(Section.class))).thenAnswer(i -> i.getArgument(0));

        // mismo horario en otro orden: no cuenta como cambio
        SectionResponseDTO result = service.updateSection(1L,
                new SectionUpdateDTO(7L, 40, List.of(THREE_BLOCKS.get(2), THREE_BLOCKS.get(0), THREE_BLOCKS.get(1))));

        assertEquals(40, result.capacity());
        verify(sectionScheduleBlockRepository, never()).deleteBySectionId(any());
        verify(teacherRepository, never()).findById(any());
        verify(courseRegistrationRepository, never()).existsBySectionId(any());
    }

    @Test
    void updateSection_newBlocks_replacesThemFlushingFirst() {
        stubUpdatableSection(section(1L, 7L, 30, 0));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(sectionScheduleBlockRepository.findBySectionId(1L)).thenReturn(threeBlockRows(1L));
        when(courseRegistrationRepository.existsBySectionId(1L)).thenReturn(false);
        when(sectionRepository.save(any(Section.class))).thenAnswer(i -> i.getArgument(0));
        List<ScheduleBlockDTO> newBlocks = List.of(
                new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.J, 4), new ScheduleBlockDTO(WeekDay.V, 6));

        SectionResponseDTO result = service.updateSection(1L, new SectionUpdateDTO(7L, 30, newBlocks));

        assertEquals(newBlocks, result.scheduleBlocks());
        InOrder inOrder = inOrder(sectionScheduleBlockRepository);
        inOrder.verify(sectionScheduleBlockRepository).deleteBySectionId(1L);
        inOrder.verify(sectionScheduleBlockRepository).flush();
        inOrder.verify(sectionScheduleBlockRepository, times(3)).save(any(SectionScheduleBlock.class));
    }

    @Test
    void updateSection_changeTeacherToActive_ok() {
        stubUpdatableSection(section(1L, 7L, 30, 0));
        when(teacherRepository.findById(8L)).thenReturn(Optional.of(teacher(8L, TeacherStatus.ACTIVE)));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(sectionScheduleBlockRepository.findBySectionId(1L)).thenReturn(threeBlockRows(1L));
        when(sectionRepository.save(any(Section.class))).thenAnswer(i -> i.getArgument(0));

        SectionResponseDTO result = service.updateSection(1L, new SectionUpdateDTO(8L, 30, THREE_BLOCKS));

        assertEquals(8L, result.teacherId());
        verify(sectionRepository).findByTeacherIdAndAcademicPeriodId(8L, 10L);
    }

    @Test
    void updateSection_changeTeacherToInactive_throws() {
        stubUpdatableSection(section(1L, 7L, 30, 0));
        when(teacherRepository.findById(8L)).thenReturn(Optional.of(teacher(8L, TeacherStatus.INACTIVE)));

        assertThrows(IllegalStateException.class,
                () -> service.updateSection(1L, new SectionUpdateDTO(8L, 30, THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_closedPeriod_throws() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section(1L, 7L, 30, 0)));
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.CLOSED)));

        assertThrows(IllegalStateException.class,
                () -> service.updateSection(1L, new SectionUpdateDTO(7L, 30, THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_capacityBelowEnrolled_throws() {
        stubUpdatableSection(section(1L, 7L, 30, 20));

        assertThrows(IllegalStateException.class,
                () -> service.updateSection(1L, new SectionUpdateDTO(7L, 19, THREE_BLOCKS)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_changeBlocksWithRegistrations_throws() {
        stubUpdatableSection(section(1L, 7L, 30, 5));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(sectionScheduleBlockRepository.findBySectionId(1L)).thenReturn(threeBlockRows(1L));
        when(courseRegistrationRepository.existsBySectionId(1L)).thenReturn(true);
        List<ScheduleBlockDTO> newBlocks = List.of(
                new ScheduleBlockDTO(WeekDay.L, 2), new ScheduleBlockDTO(WeekDay.M, 1), new ScheduleBlockDTO(WeekDay.W, 1));

        assertThrows(IllegalStateException.class,
                () -> service.updateSection(1L, new SectionUpdateDTO(7L, 30, newBlocks)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_conflictWithTeachersOtherSection_throws() {
        stubUpdatableSection(section(1L, 7L, 30, 0));
        when(subjectRepository.findById(5L)).thenReturn(Optional.of(subject(2, 2, 2)));
        when(sectionScheduleBlockRepository.findBySectionId(1L)).thenReturn(threeBlockRows(1L));
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(7L, 10L))
                .thenReturn(List.of(section(1L, 7L, 30, 0), section(2L, 7L, 30, 0)));
        when(sectionScheduleBlockRepository.findBySectionId(2L)).thenReturn(List.of(block(2L, WeekDay.J, 4)));
        List<ScheduleBlockDTO> newBlocks = List.of(
                new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.M, 1), new ScheduleBlockDTO(WeekDay.J, 4));

        assertThrows(IllegalStateException.class,
                () -> service.updateSection(1L, new SectionUpdateDTO(7L, 30, newBlocks)));
        verify(sectionRepository, never()).save(any());
    }

    // ---- eliminar ----

    @Test
    void deleteSection_withoutRegistrations_deletesSectionAndBlocks() {
        Section section = section(1L, 7L, 30, 0);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(courseRegistrationRepository.existsBySectionId(1L)).thenReturn(false);

        service.deleteSection(1L);

        verify(sectionScheduleBlockRepository).deleteBySectionId(1L);
        verify(sectionRepository).delete(section);
    }

    @Test
    void deleteSection_withRegistrations_throws() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section(1L, 7L, 30, 3)));
        when(courseRegistrationRepository.existsBySectionId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteSection(1L));
        verify(sectionRepository, never()).delete(any());
        verify(sectionScheduleBlockRepository, never()).deleteBySectionId(any());
    }
}
