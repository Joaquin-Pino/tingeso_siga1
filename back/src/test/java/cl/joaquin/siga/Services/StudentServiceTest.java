package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicRecordDTO.AcademicRecordResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStatusUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStudyPlanUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentUpdateDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.University.CourseResult;
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

import java.math.BigDecimal;
import java.util.List;
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

    private Student studentInCareer(Long careerId, Long studyPlanId, StudentStatus status) {
        Student student = existingStudent();
        student.setCareerId(careerId);
        student.setStudyPlanId(studyPlanId);
        student.setStatus(status);
        return student;
    }

    private StudyPlan planOfCareer(Long id, Long careerId) {
        StudyPlan plan = currentPlan(id);
        plan.setCareerId(careerId);
        return plan;
    }

    private void careerActiveWithCurrentPlan(Long careerId, Long planId) {
        when(careerRepository.existsById(careerId)).thenReturn(true);
        when(careerRepository.existsByIdAndStatus(careerId, CareerStatus.ACTIVE)).thenReturn(true);
        when(studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(currentPlan(planId)));
    }

    // ---- consultas ----

    @Test
    void getAll_mapsAllStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(existingStudent(), existingStudent()));

        assertEquals(2, service.getAll().size());
    }

    @Test
    void getById_returnsStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));

        StudentResponseDTO result = service.getById(1L);

        assertEquals("1-9", result.run());
        assertEquals(1L, result.careerId());
        assertEquals(7L, result.studyPlanId());
    }

    @Test
    void getById_notFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void getAcademicHistory_mapsSnapshotFields() {
        AcademicRecord record = new AcademicRecord();
        record.setId(5L);
        record.setSubjectId(10L);
        record.setAcademicPeriodId(3L);
        record.setSubjectCode("TAP101");
        record.setSubjectName("Programación");
        record.setSubjectSemester(1);
        record.setCredits(5);
        record.setPeriodCode("2026-1");
        record.setFinalGrade(new BigDecimal("5.5"));
        record.setResult(CourseResult.PASSED);
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of(record));

        List<AcademicRecordResponseDTO> result = service.getAcademicHistory(1L);

        assertEquals(1, result.size());
        AcademicRecordResponseDTO dto = result.get(0);
        assertEquals("TAP101", dto.subjectCode());
        assertEquals("2026-1", dto.periodCode());
        assertEquals(new BigDecimal("5.5"), dto.finalGrade());
        assertEquals(CourseResult.PASSED, dto.result());
    }

    @Test
    void getAcademicHistory_studentNotFound_throws() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getAcademicHistory(1L));
        verify(academicRecordRepository, never()).findByStudentId(any());
    }

    // ---- registro ----

    @Test
    void saveStudent_duplicateEmail_throws() {
        when(studentRepository.existsByEmail("a@x.cl")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void saveStudent_careerNotFound_throws() {
        when(careerRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void saveStudent_careerWithoutCurrentPlan_throws() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(careerRepository.existsByIdAndStatus(1L, CareerStatus.ACTIVE)).thenReturn(true);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    // ---- cambio de estado ----

    @Test
    void updateStatus_allowedStatuses_areApplied() {
        for (StudentStatus status : List.of(StudentStatus.REGULAR, StudentStatus.POSTPONED,
                StudentStatus.TEMPORARY_WITHDRAWAL)) {
            when(studentRepository.findById(1L))
                    .thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
            when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

            StudentResponseDTO result = service.updateStatus(1L, new StudentStatusUpdateDTO(status));

            assertEquals(status, result.status());
        }
    }

    @Test
    void updateStatus_notFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.POSTPONED)));
    }

    @Test
    void updateStatus_graduatedStudent_cannotBeModified() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.GRADUATED)));

        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.REGULAR)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStatus_eliminatedStudent_cannotBeModified() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.ELIMINATED)));

        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.REGULAR)));
        verify(studentRepository, never()).save(any());
    }

    // ---- cambio de carrera ----

    @Test
    void updateCareer_newCareer_assignsItsCurrentPlan() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
        careerActiveWithCurrentPlan(2L, 8L);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponseDTO result = service.updateCareer(1L, new StudentCareerUpdateDTO(2L));

        assertEquals(2L, result.careerId());
        assertEquals(8L, result.studyPlanId());
    }

    @Test
    void updateCareer_sameCareer_keepsStudyPlan() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponseDTO result = service.updateCareer(1L, new StudentCareerUpdateDTO(1L));

        assertEquals(7L, result.studyPlanId());
        verify(studyPlanRepository, never()).findByCareerIdAndStatus(any(), any());
    }

    @Test
    void updateCareer_withHistory_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.updateCareer(1L, new StudentCareerUpdateDTO(2L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateCareer_inactiveCareer_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
        when(careerRepository.existsById(2L)).thenReturn(true);
        when(careerRepository.existsByIdAndStatus(2L, CareerStatus.ACTIVE)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCareer(1L, new StudentCareerUpdateDTO(2L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateCareer_notFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateCareer(1L, new StudentCareerUpdateDTO(2L)));
    }

    // ---- cambio de plan de estudios ----

    @Test
    void updateStudyPlan_planOfSameCareer_isAssigned() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
        when(studyPlanRepository.findById(6L)).thenReturn(Optional.of(planOfCareer(6L, 1L)));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponseDTO result = service.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L));

        assertEquals(6L, result.studyPlanId());
        assertEquals(1L, result.careerId());
    }

    @Test
    void updateStudyPlan_planOfOtherCareer_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
        when(studyPlanRepository.findById(6L)).thenReturn(Optional.of(planOfCareer(6L, 2L)));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudyPlan_planNotFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(studentInCareer(1L, 7L, StudentStatus.REGULAR)));
        when(studyPlanRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)));
    }

    @Test
    void updateStudyPlan_withRegistrations_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(courseRegistrationRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudyPlan_withHistory_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudyPlan_studentNotFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)));
    }

    // ---- eliminación ----

    @Test
    void deleteStudent_withoutDependencies_deletes() {
        Student student = existingStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }

    @Test
    void deleteStudent_withRegistrations_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(courseRegistrationRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteStudent(1L));
        verify(studentRepository, never()).delete(any());
    }

    @Test
    void deleteStudent_notFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteStudent(1L));
    }

    // ---- usados por el cierre de período ----

    @Test
    void graduate_setsGraduated() {
        Student student = studentInCareer(1L, 7L, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.graduate(1L);

        assertEquals(StudentStatus.GRADUATED, student.getStatus());
        verify(studentRepository).save(student);
    }

    @Test
    void eliminate_setsEliminated() {
        Student student = studentInCareer(1L, 7L, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.eliminate(1L);

        assertEquals(StudentStatus.ELIMINATED, student.getStatus());
        verify(studentRepository).save(student);
    }

    @Test
    void graduateAndEliminate_notFound_throw() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.graduate(1L));
        assertThrows(NotFoundException.class, () -> service.eliminate(1L));
    }
}
