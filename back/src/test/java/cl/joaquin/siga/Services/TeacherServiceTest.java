package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherResponseDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherStatusUpdateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherUpdateDTO;
import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Entities.People.TeacherStatus;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock SectionRepository sectionRepository;
    @Mock AcademicPeriodRepository academicPeriodRepository;
    @InjectMocks TeacherService service;

    private Teacher existingTeacher() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setRun("1-9");
        teacher.setFullName("Pedro");
        teacher.setEmail("p@x.cl");
        teacher.setProfessionalTitle("Ingeniero");
        teacher.setAcademicDegree(AcademicDegree.BACHELOR);
        teacher.setStatus(TeacherStatus.ACTIVE);
        return teacher;
    }

    private Section sectionInPeriod(Long periodId) {
        Section section = new Section();
        section.setAcademicPeriodId(periodId);
        return section;
    }

    private TeacherCreateDTO createDTO() {
        return new TeacherCreateDTO("kc", "1-9", "Pedro", "p@x.cl", "Ingeniero", AcademicDegree.MASTER);
    }

    @Test
    void getAll_mapsAllTeachers() {
        when(teacherRepository.findAll()).thenReturn(List.of(existingTeacher(), existingTeacher()));

        assertEquals(2, service.getAll().size());
    }

    @Test
    void getById_returnsTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));

        assertEquals("1-9", service.getById(1L).run());
    }

    @Test
    void getById_notFound_throws() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void saveTeacher_isActive() {
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        TeacherResponseDTO result = service.saveTeacher(createDTO());

        assertEquals(TeacherStatus.ACTIVE, result.status());
        assertEquals(AcademicDegree.MASTER, result.academicDegree());
    }

    @Test
    void saveTeacher_duplicateRun_throws() {
        when(teacherRepository.existsByRun("1-9")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.saveTeacher(createDTO()));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_duplicateEmail_throws() {
        when(teacherRepository.existsByEmail("p@x.cl")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.saveTeacher(createDTO()));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_updatesFields_keepsRun() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));
        when(teacherRepository.existsByEmail("q@x.cl")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        TeacherResponseDTO result = service.updateTeacher(1L,
                new TeacherUpdateDTO("Pedro P", "q@x.cl", "Magíster", AcademicDegree.DOCTORATE));

        assertEquals("1-9", result.run());
        assertEquals("Pedro P", result.fullName());
        assertEquals("q@x.cl", result.email());
        assertEquals(AcademicDegree.DOCTORATE, result.academicDegree());
    }

    @Test
    void updateTeacher_sameEmail_doesNotCheckUniqueness() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        service.updateTeacher(1L, new TeacherUpdateDTO("Pedro P", "p@x.cl", "Ingeniero", AcademicDegree.BACHELOR));

        verify(teacherRepository, never()).existsByEmail(any());
    }

    @Test
    void updateTeacher_emailTaken_throws() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));
        when(teacherRepository.existsByEmail("q@x.cl")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.updateTeacher(1L,
                new TeacherUpdateDTO("Pedro", "q@x.cl", "Ingeniero", AcademicDegree.BACHELOR)));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateStatus_inactiveWithSectionInOpenPeriod_throws() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));
        when(sectionRepository.findByTeacherId(1L)).thenReturn(List.of(sectionInPeriod(10L), sectionInPeriod(20L)));
        when(academicPeriodRepository.existsByIdAndStatus(10L, PeriodStatus.OPEN)).thenReturn(false);
        when(academicPeriodRepository.existsByIdAndStatus(20L, PeriodStatus.OPEN)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(1L, new TeacherStatusUpdateDTO(TeacherStatus.INACTIVE)));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateStatus_inactiveWithOnlyClosedPeriods_ok() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));
        when(sectionRepository.findByTeacherId(1L)).thenReturn(List.of(sectionInPeriod(10L)));
        when(academicPeriodRepository.existsByIdAndStatus(10L, PeriodStatus.OPEN)).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        TeacherResponseDTO result = service.updateStatus(1L, new TeacherStatusUpdateDTO(TeacherStatus.INACTIVE));

        assertEquals(TeacherStatus.INACTIVE, result.status());
    }

    @Test
    void updateStatus_toActive_doesNotCheckSections() {
        Teacher teacher = existingTeacher();
        teacher.setStatus(TeacherStatus.INACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        TeacherResponseDTO result = service.updateStatus(1L, new TeacherStatusUpdateDTO(TeacherStatus.ACTIVE));

        assertEquals(TeacherStatus.ACTIVE, result.status());
        verify(sectionRepository, never()).findByTeacherId(any());
    }

    @Test
    void deleteTeacher_withoutSections_deletes() {
        Teacher teacher = existingTeacher();
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sectionRepository.existsByTeacherId(1L)).thenReturn(false);

        service.deleteTeacher(1L);

        verify(teacherRepository).delete(teacher);
    }

    @Test
    void deleteTeacher_withSections_throws() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existingTeacher()));
        when(sectionRepository.existsByTeacherId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteTeacher(1L));
        verify(teacherRepository, never()).delete(any());
    }
}
