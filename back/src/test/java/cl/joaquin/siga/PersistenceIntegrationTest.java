package cl.joaquin.siga;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectUpdateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SectionScheduleBlockRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import cl.joaquin.siga.Services.AcademicPeriodService;
import cl.joaquin.siga.Services.CareerService;
import cl.joaquin.siga.Services.SectionService;
import cl.joaquin.siga.Services.StudentService;
import cl.joaquin.siga.Services.StudyPlanService;
import cl.joaquin.siga.Services.SubjectService;
import cl.joaquin.siga.Services.TeacherService;
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
    @Autowired TeacherService teacherService;
    @Autowired AcademicPeriodService academicPeriodService;
    @Autowired SectionService sectionService;

    @Autowired CareerRepository careerRepository;
    @Autowired StudyPlanRepository studyPlanRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired AcademicPeriodRepository academicPeriodRepository;
    @Autowired SectionRepository sectionRepository;
    @Autowired SectionScheduleBlockRepository sectionScheduleBlockRepository;

    private Long careerId;

    @BeforeEach
    void cleanDatabaseAndCreateCareer() {
        sectionScheduleBlockRepository.deleteAll();
        sectionRepository.deleteAll();
        academicPeriodRepository.deleteAll();
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

    // ---- oferta académica y secciones ----

    // asignatura 2-2-0 (4 horas, 2 bloques), docente activo y período abierto
    private record OfferingFixture(Long subjectId, Long otherSubjectId, Long teacherId, Long periodId) {
    }

    private OfferingFixture offeringFixture() {
        Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();
        Long subjectId = createSubject(planId, "TAP102", 1, Set.of()).id();
        Long otherSubjectId = createSubject(planId, "TAP105", 1, Set.of()).id();
        Long teacherId = teacherService.saveTeacher(new TeacherCreateDTO(
                "kc-t", "2-7", "Pedro", "p@x.cl", "Ingeniero", AcademicDegree.MASTER)).id();
        Long periodId = academicPeriodService.createPeriod(new AcademicPeriodCreateDTO(2027, Semester.FIRST)).id();
        return new OfferingFixture(subjectId, otherSubjectId, teacherId, periodId);
    }

    private static List<ScheduleBlockDTO> blocks(ScheduleBlockDTO... blocks) {
        return List.of(blocks);
    }

    @Test
    void createSection_persistsBlocksAndStartsEmpty() {
        OfferingFixture f = offeringFixture();

        SectionResponseDTO created = sectionService.createSection(new SectionCreateDTO(f.subjectId(), f.periodId(),
                f.teacherId(), 30, blocks(new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.W, 1))));

        assertEquals(0, sectionRepository.findById(created.id()).orElseThrow().getEnrolledCount());
        assertEquals(2, sectionScheduleBlockRepository.findBySectionId(created.id()).size());
        assertEquals(1, sectionService.getAllPeriodSections(f.periodId()).size());
    }

    @Test
    void uniqueConstraint_rejectsSecondSectionOfSubjectInPeriod() {
        // simula dos réplicas que pasan el existsBy a la vez: la BD es la que corta
        OfferingFixture f = offeringFixture();
        sectionRepository.saveAndFlush(rawSection(f));

        assertThrows(DataIntegrityViolationException.class, () -> sectionRepository.saveAndFlush(rawSection(f)));
    }

    @Test
    void updateSection_keepingOneBlock_doesNotViolateUnique() {
        OfferingFixture f = offeringFixture();
        Long sectionId = sectionService.createSection(new SectionCreateDTO(f.subjectId(), f.periodId(),
                f.teacherId(), 30, blocks(new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.W, 1)))).id();

        // L-1 se mantiene: sin el flush intermedio chocaría con uk_section_schedule_block_section_day_module
        List<ScheduleBlockDTO> newBlocks = blocks(new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.J, 3));
        sectionService.updateSection(sectionId, new SectionUpdateDTO(f.teacherId(), 30, newBlocks));

        assertEquals(Set.copyOf(newBlocks), Set.copyOf(sectionService.getById(sectionId).scheduleBlocks()));
    }

    @Test
    void createSection_teacherScheduleConflict_isRejected() {
        OfferingFixture f = offeringFixture();
        sectionService.createSection(new SectionCreateDTO(f.subjectId(), f.periodId(), f.teacherId(), 30,
                blocks(new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.W, 1))));

        assertThrows(IllegalStateException.class, () -> sectionService.createSection(new SectionCreateDTO(
                f.otherSubjectId(), f.periodId(), f.teacherId(), 30,
                blocks(new ScheduleBlockDTO(WeekDay.W, 1), new ScheduleBlockDTO(WeekDay.V, 2)))));
        assertEquals(1, sectionRepository.findByTeacherId(f.teacherId()).size());
    }

    @Test
    void deleteSection_removesItsBlocks() {
        OfferingFixture f = offeringFixture();
        Long sectionId = sectionService.createSection(new SectionCreateDTO(f.subjectId(), f.periodId(),
                f.teacherId(), 30, blocks(new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.W, 1)))).id();

        sectionService.deleteSection(sectionId);

        assertFalse(sectionRepository.existsById(sectionId));
        assertTrue(sectionScheduleBlockRepository.findBySectionId(sectionId).isEmpty());
        // sin secciones el período ya se puede eliminar
        assertDoesNotThrow(() -> academicPeriodService.deletePeriod(f.periodId()));
    }

    private static Section rawSection(OfferingFixture f) {
        Section section = new Section();
        section.setSubjectId(f.subjectId());
        section.setAcademicPeriodId(f.periodId());
        section.setTeacherId(f.teacherId());
        section.setCapacity(30);
        section.setEnrolledCount(0);
        return section;
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
