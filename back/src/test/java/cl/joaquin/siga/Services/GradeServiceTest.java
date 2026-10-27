package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.SectionRegistrationResponseDTO;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.GradeUpdateDTO;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    private static final Long TEACHER_ID = 2L;
    private static final Long SECTION_ID = 5L;
    private static final Long PERIOD_ID = 10L;

    @Mock CourseRegistrationRepository courseRegistrationRepository;
    @Mock SectionRepository sectionRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock StudentRepository studentRepository;
    @Mock AcademicPeriodRepository academicPeriodRepository;
    @InjectMocks GradeService service;

    private AcademicPeriod period;
    private CourseRegistration registration;

    @BeforeEach
    void setUp() {
        Section section = new Section();
        section.setId(SECTION_ID);
        section.setTeacherId(TEACHER_ID);
        section.setAcademicPeriodId(PERIOD_ID);
        period = new AcademicPeriod();
        period.setId(PERIOD_ID);
        period.setStatus(PeriodStatus.OPEN);
        registration = new CourseRegistration();
        registration.setId(7L);
        registration.setStudentId(3L);
        registration.setSectionId(SECTION_ID);

        lenient().when(teacherRepository.existsById(TEACHER_ID)).thenReturn(true);
        lenient().when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        lenient().when(academicPeriodRepository.findById(PERIOD_ID)).thenAnswer(i -> Optional.of(period));
        lenient().when(courseRegistrationRepository.findById(7L)).thenReturn(Optional.of(registration));
        lenient().when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registerGrade_savesGradeAndLeavesResultEmpty() {
        CourseRegistrationResponseDTO result =
                service.registerGrade(TEACHER_ID, SECTION_ID, 7L, new GradeUpdateDTO(new BigDecimal("5.5")));

        assertEquals(new BigDecimal("5.5"), result.grade());
        assertNull(result.result());
        assertEquals(new BigDecimal("5.5"), registration.getGrade());
    }

    @Test
    void registerGrade_canCorrectAnExistingGrade() {
        registration.setGrade(new BigDecimal("3.0"));

        service.registerGrade(TEACHER_ID, SECTION_ID, 7L, new GradeUpdateDTO(new BigDecimal("4.5")));

        assertEquals(new BigDecimal("4.5"), registration.getGrade());
    }

    @Test
    void registerGrade_teacherNotFound_throws() {
        when(teacherRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.registerGrade(99L, SECTION_ID, 7L, new GradeUpdateDTO(new BigDecimal("5.0"))));
    }

    @Test
    void registerGrade_sectionNotFound_throws() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.registerGrade(TEACHER_ID, 99L, 7L, new GradeUpdateDTO(new BigDecimal("5.0"))));
    }

    @Test
    void registerGrade_teacherIsNotResponsible_isRejected() {
        when(teacherRepository.existsById(8L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.registerGrade(8L, SECTION_ID, 7L, new GradeUpdateDTO(new BigDecimal("5.0"))));
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void registerGrade_closedPeriod_isRejected() {
        period.setStatus(PeriodStatus.CLOSED);

        assertThrows(IllegalStateException.class,
                () -> service.registerGrade(TEACHER_ID, SECTION_ID, 7L, new GradeUpdateDTO(new BigDecimal("5.0"))));
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void registerGrade_registrationNotFound_throws() {
        when(courseRegistrationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.registerGrade(TEACHER_ID, SECTION_ID, 99L, new GradeUpdateDTO(new BigDecimal("5.0"))));
    }

    @Test
    void registerGrade_registrationOfAnotherSection_throwsNotFound() {
        registration.setSectionId(6L);

        assertThrows(NotFoundException.class,
                () -> service.registerGrade(TEACHER_ID, SECTION_ID, 7L, new GradeUpdateDTO(new BigDecimal("5.0"))));
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void getSectionRegistrations_returnsEnrolled() {
        Student student = new Student();
        student.setId(3L);
        student.setRun("12.345.678-9");
        student.setFullName("Ana Pérez");
        when(courseRegistrationRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(registration));
        when(studentRepository.findAllById(List.of(3L))).thenReturn(List.of(student));

        List<SectionRegistrationResponseDTO> result = service.getSectionRegistrations(TEACHER_ID, SECTION_ID);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).studentId());
        assertEquals("12.345.678-9", result.get(0).studentRun());
        assertEquals("Ana Pérez", result.get(0).studentFullName());
    }

    @Test
    void getSectionRegistrations_teacherIsNotResponsible_isRejected() {
        when(teacherRepository.existsById(8L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.getSectionRegistrations(8L, SECTION_ID));
    }
}
