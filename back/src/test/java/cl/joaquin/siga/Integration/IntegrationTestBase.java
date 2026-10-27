package cl.joaquin.siga.Integration;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SectionScheduleBlockRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import cl.joaquin.siga.Services.AcademicPeriodService;
import cl.joaquin.siga.Services.CareerService;
import cl.joaquin.siga.Services.KeycloakAdminService;
import cl.joaquin.siga.Services.PeriodEnrollmentService;
import cl.joaquin.siga.Services.SectionService;
import cl.joaquin.siga.Services.StudentService;
import cl.joaquin.siga.Services.StudyPlanService;
import cl.joaquin.siga.Services.SubjectService;
import cl.joaquin.siga.Services.TeacherService;
import cl.joaquin.siga.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

// Pruebas de integración de procesos: request HTTP real (MockMvc) con los filtros de seguridad activos,
// Controller → Service → Repository y Postgres real (Testcontainers). Solo Keycloak está simulado: las
// cuentas se "crean" con un id aleatorio, que es el sub de los JWT que arman los tests
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @MockitoBean protected KeycloakAdminService keycloakAdminService;

    @Autowired protected CareerService careerService;
    @Autowired protected StudyPlanService studyPlanService;
    @Autowired protected SubjectService subjectService;
    @Autowired protected StudentService studentService;
    @Autowired protected TeacherService teacherService;
    @Autowired protected AcademicPeriodService academicPeriodService;
    @Autowired protected PeriodEnrollmentService periodEnrollmentService;
    @Autowired protected SectionService sectionService;

    @Autowired protected CareerRepository careerRepository;
    @Autowired protected StudyPlanRepository studyPlanRepository;
    @Autowired protected SubjectRepository subjectRepository;
    @Autowired protected SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected AcademicPeriodRepository academicPeriodRepository;
    @Autowired protected PeriodEnrollmentRepository periodEnrollmentRepository;
    @Autowired protected SectionRepository sectionRepository;
    @Autowired protected SectionScheduleBlockRepository sectionScheduleBlockRepository;
    @Autowired protected CourseRegistrationRepository courseRegistrationRepository;
    @Autowired protected AcademicRecordRepository academicRecordRepository;

    protected Long careerId;
    protected Long planId;
    private int counter;

    @BeforeEach
    void cleanDatabaseAndCreateCareer() {
        when(keycloakAdminService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> UUID.randomUUID().toString());
        academicRecordRepository.deleteAll();
        courseRegistrationRepository.deleteAll();
        periodEnrollmentRepository.deleteAll();
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
        planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, "2026.1")).id();
    }

    // ---- JWT por rol; el sub es el keycloakId de la persona, que es lo que compara @currentUser ----

    protected RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    protected RequestPostProcessor asStudent(Long studentId) {
        String sub = studentRepository.findById(studentId).orElseThrow().getKeycloakId();
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    protected RequestPostProcessor asTeacher(Long teacherId) {
        String sub = teacherRepository.findById(teacherId).orElseThrow().getKeycloakId();
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_TEACHER"));
    }

    // ---- datos de prueba, creados por los Services para que cumplan las mismas reglas que en producción ----

    // asignatura de 2 horas semanales (un bloque horario)
    protected Long subject(String code, int semester, Long... prerequisiteIds) {
        return subjectService.createSubject(new SubjectCreateDTO(
                planId, code, "Asignatura " + code, semester, 2, 0, 0, 5, Set.of(prerequisiteIds))).id();
    }

    protected Long teacher() {
        int n = ++counter;
        return teacherService.saveTeacher(new TeacherCreateDTO("Temporal1!", "T-" + n, "Docente " + n,
                "docente" + n + "@ipt.cl", "Ingeniero", AcademicDegree.MASTER)).id();
    }

    protected Long student() {
        int n = ++counter;
        return studentService.saveStudent(new StudentCreateDTO("Temporal1!", "S-" + n, "Estudiante " + n,
                "estudiante" + n + "@ipt.cl", careerId)).id();
    }

    protected Long period(int year, Semester semester) {
        return academicPeriodService.createPeriod(new AcademicPeriodCreateDTO(year, semester)).id();
    }

    protected void enroll(Long periodId, Long studentId) {
        periodEnrollmentService.enroll(periodId, new PeriodEnrollmentCreateDTO(studentId));
    }

    protected Long section(Long subjectId, Long periodId, Long teacherId, int capacity, ScheduleBlockDTO... blocks) {
        return sectionService.createSection(
                new SectionCreateDTO(subjectId, periodId, teacherId, capacity, List.of(blocks))).id();
    }

    protected static ScheduleBlockDTO block(WeekDay day, int module) {
        return new ScheduleBlockDTO(day, module);
    }

    protected static String sectionIds(Long... ids) {
        return "{\"sectionIds\": " + List.of(ids) + "}";
    }
}
