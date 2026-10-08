package cl.joaquin.siga;

import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectUpdateDTO;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import cl.joaquin.siga.Services.CareerService;
import cl.joaquin.siga.Services.StudentService;
import cl.joaquin.siga.Services.StudyPlanService;
import cl.joaquin.siga.Services.SubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Lo que los tests con mocks no pueden probar: el índice parcial de schema.sql, las unique constraints
// (la defensa real entre réplicas) y el orden de flush que los Services necesitan para no violarlas
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PersistenceIntegrationTest {

    @Autowired CareerService careerService;
    @Autowired StudyPlanService studyPlanService;
    @Autowired SubjectService subjectService;
    @Autowired StudentService studentService;

    @Autowired CareerRepository careerRepository;
    @Autowired StudyPlanRepository studyPlanRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherRepository teacherRepository;

    private Long careerId;

    @BeforeEach
    void cleanDatabaseAndCreateCareer() {
        subjectPrerequisiteRepository.deleteAll();
        subjectRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        studyPlanRepository.deleteAll();
        careerRepository.deleteAll();

        careerId = careerService.createCareer(new CareerCreateDTO("TAP", "Técnico", "desc", 2020, 100)).id();
    }

    private StudyPlan rawPlan(String code, StudyPlanStatus status) {
        StudyPlan plan = new StudyPlan();
        plan.setCareerId(careerId);
        plan.setCode(code);
        plan.setStatus(status);
        return plan;
    }

    private SubjectResponseDTO createSubject(Long studyPlanId, String code, int semester, Set<Long> prerequisiteIds) {
        return subjectService.createSubject(
                new SubjectCreateDTO(studyPlanId, code, code, semester, 2, 2, 0, 5, prerequisiteIds));
    }

    // ---- planes de estudio: un solo vigente por carrera ----

    @Test
    void partialIndex_rejectsTwoCurrentPlansInSameCareer() {
        studyPlanRepository.saveAndFlush(rawPlan("2020.1", StudyPlanStatus.CURRENT));

        assertThrows(DataIntegrityViolationException.class,
                () -> studyPlanRepository.saveAndFlush(rawPlan("2026.1", StudyPlanStatus.CURRENT)));
    }

    @Test
    void partialIndex_allowsManyNotCurrentPlans() {
        studyPlanRepository.saveAndFlush(rawPlan("2018.1", StudyPlanStatus.NOT_CURRENT));
        studyPlanRepository.saveAndFlush(rawPlan("2019.1", StudyPlanStatus.NOT_CURRENT));

        assertDoesNotThrow(() -> studyPlanRepository.saveAndFlush(rawPlan("2020.1", StudyPlanStatus.CURRENT)));
    }

    @Test
    void createStudyPlan_twice_leavesExactlyOneCurrent() {
        StudyPlanResponseDTO first = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2020.1"));
        StudyPlanResponseDTO second = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1"));

        assertEquals(StudyPlanStatus.NOT_CURRENT, studyPlanRepository.findById(first.id()).orElseThrow().getStatus());
        assertEquals(StudyPlanStatus.CURRENT, studyPlanRepository.findById(second.id()).orElseThrow().getStatus());
        assertEquals(second.id(),
                studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT).orElseThrow().getId());
    }

    @Test
    void uniqueConstraint_rejectsDuplicatePlanCodeInCareer() {
        studyPlanRepository.saveAndFlush(rawPlan("2020.1", StudyPlanStatus.NOT_CURRENT));

        assertThrows(DataIntegrityViolationException.class,
                () -> studyPlanRepository.saveAndFlush(rawPlan("2020.1", StudyPlanStatus.NOT_CURRENT)));
    }

    // ---- asignaturas y prerrequisitos ----

    @Test
    void updateSubject_keepingSamePrerequisite_doesNotViolateUnique() {
        Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();
        Long prerequisiteId = createSubject(planId, "TAP101", 1, Set.of()).id();
        Long subjectId = createSubject(planId, "TAP201", 2, Set.of(prerequisiteId)).id();

        // se borran y reinsertan las mismas filas: sin el flush intermedio chocaría con el unique
        SubjectResponseDTO result = subjectService.updateSubject(subjectId,
                new SubjectUpdateDTO("TAP201", "Renombrada", 2, 3, 2, 0, 5, Set.of(prerequisiteId)));

        assertEquals("Renombrada", result.name());
        assertEquals(List.of(prerequisiteId), subjectService.getById(subjectId).prerequisiteIds());
    }

    @Test
    void deleteSubject_removesItsPrerequisiteRows() {
        Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();
        Long prerequisiteId = createSubject(planId, "TAP101", 1, Set.of()).id();
        Long subjectId = createSubject(planId, "TAP201", 2, Set.of(prerequisiteId)).id();

        subjectService.deleteSubject(subjectId);

        assertFalse(subjectRepository.existsById(subjectId));
        assertTrue(subjectPrerequisiteRepository.findBySubjectId(subjectId).isEmpty());
        // ahora el prerrequisito ya no es requerido por nadie y se puede borrar
        assertDoesNotThrow(() -> subjectService.deleteSubject(prerequisiteId));
    }

    @Test
    void createSubject_duplicateCodeInPlan_isRejected() {
        Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();
        createSubject(planId, "TAP101", 1, Set.of());

        assertThrows(IllegalArgumentException.class, () -> createSubject(planId, "TAP101", 1, Set.of()));
        assertEquals(1, subjectRepository.findByStudyPlanId(planId).size());
    }

    // ---- estudiantes ----

    @Test
    void saveStudent_persistsLinkedToCurrentPlan() {
        Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();

        Long studentId = studentService.saveStudent(
                new StudentCreateDTO("kc-1", "1-9", "Ana", "a@x.cl", careerId)).id();

        Student saved = studentRepository.findById(studentId).orElseThrow();
        assertEquals(planId, saved.getStudyPlanId());
        assertEquals(StudentStatus.REGULAR, saved.getStatus());
        assertTrue(studentRepository.existsByCareerId(careerId));
        assertTrue(studentRepository.findByKeycloakId("kc-1").isPresent());
    }

    @Test
    void uniqueConstraint_rejectsDuplicateStudentRun() {
        // simula dos réplicas que pasan el existsByRun a la vez: la BD es la que corta
        Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();
        studentRepository.saveAndFlush(student("kc-1", "1-9", "a@x.cl", planId));

        assertThrows(DataIntegrityViolationException.class,
                () -> studentRepository.saveAndFlush(student("kc-2", "1-9", "b@x.cl", planId)));
    }

    @Test
    void deleteCareer_withStudyPlan_isRejected() {
        studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1"));

        assertThrows(IllegalStateException.class, () -> careerService.deleteCareer(careerId));
        assertTrue(careerRepository.existsById(careerId));
    }

    private Student student(String keycloakId, String run, String email, Long planId) {
        Student student = new Student();
        student.setKeycloakId(keycloakId);
        student.setRun(run);
        student.setFullName("Ana");
        student.setEmail(email);
        student.setCareerId(careerId);
        student.setStudyPlanId(planId);
        student.setStatus(StudentStatus.REGULAR);
        return student;
    }
}
