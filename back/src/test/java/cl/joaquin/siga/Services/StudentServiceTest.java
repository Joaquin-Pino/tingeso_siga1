package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.StudentDTO.StudentCareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStatusUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentUpdateDTO;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.CareerStatus;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock CareerRepository careerRepository;
    @Mock StudyPlanRepository studyPlanRepository;
    @Mock CourseRegistrationRepository courseRegistrationRepository;
    @Mock AcademicRecordRepository academicRecordRepository;
    @InjectMocks StudentService service;

    private StudyPlan currentPlan(Long id) {
        StudyPlan plan = new StudyPlan();
        plan.setId(id);
        plan.setStatus(StudyPlanStatus.CURRENT);
        return plan;
    }

    @Test
    void saveStudent_isRegularAndLinkedToCurrentPlan() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(careerRepository.existsByIdAndStatus(1L, CareerStatus.ACTIVE)).thenReturn(true);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(currentPlan(7L)));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponseDTO result = service.saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L));

        assertEquals(StudentStatus.REGULAR, result.status());
        assertEquals(7L, result.studyPlanId());
    }

    @Test
    void saveStudent_inactiveCareer_throws() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(careerRepository.existsByIdAndStatus(1L, CareerStatus.ACTIVE)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void saveStudent_duplicateRun_throws() {
        when(studentRepository.existsByRun("1-9")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L)));
    }

    @Test
    void updateStatus_graduatedNotAssignableManually() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.GRADUATED)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.ELIMINATED)));
    }

    @Test
    void updateCareer_withRegistrations_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(courseRegistrationRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.updateCareer(1L, new StudentCareerUpdateDTO(2L)));
    }

    @Test
    void deleteStudent_withHistory_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteStudent(1L));
        verify(studentRepository, never()).delete(any());
    }

    private Student existingStudent() {
        Student student = new Student();
        student.setId(1L);
        student.setRun("1-9");
        student.setFullName("Ana");
        student.setEmail("a@x.cl");
        return student;
    }

    @Test
    void updateStudent_updatesNameAndEmail_keepsRun() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent()));
        when(studentRepository.existsByEmail("b@x.cl")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponseDTO result = service.updateStudent(1L, new StudentUpdateDTO("Ana María", "b@x.cl"));

        assertEquals("Ana María", result.fullName());
        assertEquals("b@x.cl", result.email());
        assertEquals("1-9", result.run());
    }

    @Test
    void updateStudent_sameEmail_skipsUniquenessCheck() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent()));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        service.updateStudent(1L, new StudentUpdateDTO("Ana María", "a@x.cl"));

        verify(studentRepository, never()).existsByEmail(any());
    }

    @Test
    void updateStudent_emailTakenByAnother_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent()));
        when(studentRepository.existsByEmail("b@x.cl")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStudent(1L, new StudentUpdateDTO("Ana", "b@x.cl")));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudent_notFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateStudent(1L, new StudentUpdateDTO("Ana", "a@x.cl")));
    }
}
