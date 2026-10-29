package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.Career;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Datos de demo para poder probar el flujo completo: 4 docentes, 4 estudiantes, un período abierto con la oferta
// del primer semestre de las 3 carreras y los estudiantes matriculados en el período. Pasa por los mismos Services
// que la API (las cuentas de Keycloak se crean igual), así que necesita el catálogo del SeedDataService ya cargado.
// No es una sola transacción porque las cuentas de Keycloak no se pueden revertir: cada paso se salta lo que ya existe
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    // contraseña temporal de todas las cuentas de demo; Keycloak pide cambiarla en el primer ingreso
    static final String TEMPORARY_PASSWORD = "Siga2026!";
    static final int PERIOD_YEAR = 2026;
    static final Semester PERIOD_SEMESTER = Semester.SECOND;
    private static final int SECTION_CAPACITY = 30;
    private static final int DAYS = WeekDay.values().length;
    private static final int MODULES_PER_DAY = 6;
    private static final int HOURS_PER_BLOCK = 2;

    private record TeacherSeed(String run, String fullName, String email, String title, AcademicDegree degree) {
    }

    private record StudentSeed(String run, String fullName, String email, String careerCode) {
    }

    private static final List<TeacherSeed> TEACHERS = List.of(
            new TeacherSeed("10.111.111-1", "Carolina Muñoz Soto", "carolina.munoz@ipt.cl",
                    "Ingeniera Civil Informática", AcademicDegree.MASTER),
            new TeacherSeed("10.222.222-2", "Andrés Fuentes Rojas", "andres.fuentes@ipt.cl",
                    "Ingeniero en Computación", AcademicDegree.BACHELOR),
            new TeacherSeed("10.333.333-3", "Valentina Araya Pérez", "valentina.araya@ipt.cl",
                    "Ingeniera Civil en Informática", AcademicDegree.DOCTORATE),
            new TeacherSeed("10.444.444-4", "Matías Contreras Vera", "matias.contreras@ipt.cl",
                    "Ingeniero Civil en Telecomunicaciones", AcademicDegree.MASTER));

    private static final List<StudentSeed> STUDENTS = List.of(
            new StudentSeed("20.111.111-1", "Sofía Herrera Díaz", "sofia.herrera@ipt.cl", "10450"),
            new StudentSeed("20.222.222-2", "Diego Sandoval Mora", "diego.sandoval@ipt.cl", "10450"),
            new StudentSeed("20.333.333-3", "Camila Reyes Lagos", "camila.reyes@ipt.cl", "10451"),
            new StudentSeed("20.444.444-4", "Felipe Vargas Núñez", "felipe.vargas@ipt.cl", "10452"));

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final PeriodEnrollmentRepository periodEnrollmentRepository;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final AcademicPeriodService academicPeriodService;
    private final SectionService sectionService;
    private final PeriodEnrollmentService periodEnrollmentService;

    // solo carga si no hay docentes ni estudiantes (no mezcla con datos reales); devuelve false si no hizo nada
    public boolean seedIfEmpty() {
        if (teacherRepository.count() > 0 || studentRepository.count() > 0) {
            return false;
        }
        if (careerRepository.count() == 0) {
            log.warn("Datos de demo omitidos: falta el catálogo (carreras y asignaturas)");
            return false;
        }

        List<Long> teacherIds = seedTeachers();
        List<Long> studentIds = seedStudents();
        Long periodId = seedPeriod();
        int sections = seedSections(periodId, teacherIds);
        for (Long studentId : studentIds) {
            if (!periodEnrollmentRepository.existsByStudentIdAndAcademicPeriodId(studentId, periodId)) {
                periodEnrollmentService.enroll(periodId, new PeriodEnrollmentCreateDTO(studentId));
            }
        }
        log.info("Datos de demo: {} docentes, {} estudiantes, período {}-{} con {} secciones",
                teacherIds.size(), studentIds.size(), PERIOD_YEAR, PERIOD_SEMESTER.getNumber(), sections);
        return true;
    }

    private List<Long> seedTeachers() {
        List<Long> ids = new ArrayList<>();
        for (TeacherSeed teacher : TEACHERS) {
            ids.add(teacherService.saveTeacher(new TeacherCreateDTO(TEMPORARY_PASSWORD, teacher.run(),
                    teacher.fullName(), teacher.email(), teacher.title(), teacher.degree())).id());
        }
        return ids;
    }

    private List<Long> seedStudents() {
        List<Long> ids = new ArrayList<>();
        for (StudentSeed student : STUDENTS) {
            Long careerId = findCareer(student.careerCode()).getId();
            ids.add(studentService.saveStudent(new StudentCreateDTO(TEMPORARY_PASSWORD, student.run(),
                    student.fullName(), student.email(), careerId)).id());
        }
        return ids;
    }

    private Long seedPeriod() {
        if (!academicPeriodRepository.existsByYearAndSemester(PERIOD_YEAR, PERIOD_SEMESTER)) {
            return academicPeriodService.createPeriod(new AcademicPeriodCreateDTO(PERIOD_YEAR, PERIOD_SEMESTER)).id();
        }
        for (AcademicPeriod period : academicPeriodRepository.findAll()) {
            if (period.getYear() == PERIOD_YEAR && period.getSemester() == PERIOD_SEMESTER) {
                return period.getId();
            }
        }
        throw new IllegalStateException("No se encontró el período de demo");
    }

    // oferta del primer semestre de cada carrera. Dentro de una carrera los bloques no se repiten (un estudiante puede
    // tomar todo sin topes) y cada docente se elige entre los que no tienen ese bloque ocupado en otra carrera
    private int seedSections(Long periodId, List<Long> teacherIds) {
        List<Set<ScheduleBlockDTO>> busyBlocks = new ArrayList<>();
        for (int i = 0; i < teacherIds.size(); i++) {
            busyBlocks.add(new HashSet<>());
        }

        int created = 0;
        int next = 0; // el docente por el que parte la búsqueda rota para repartir la carga
        for (Career career : careerRepository.findAll()) {
            StudyPlan plan = studyPlanRepository.findByCareerIdAndStatus(career.getId(), StudyPlanStatus.CURRENT)
                    .orElseThrow(() -> new IllegalStateException("La carrera no tiene plan vigente: " + career.getCode()));

            int slot = 0;
            for (Subject subject : subjectRepository.findByStudyPlanId(plan.getId())) {
                if (subject.getSemester() != 1
                        || sectionRepository.existsBySubjectIdAndAcademicPeriodId(subject.getId(), periodId)) {
                    continue;
                }
                int totalHours = subject.getWeeklyHours().getTheoryHours()
                        + subject.getWeeklyHours().getExerciseHours() + subject.getWeeklyHours().getLabHours();
                List<ScheduleBlockDTO> blocks = new ArrayList<>();
                for (int i = 0; i < totalHours / HOURS_PER_BLOCK; i++) {
                    blocks.add(blockAt(slot++));
                }

                int teacherIndex = pickFreeTeacher(busyBlocks, blocks, next);
                busyBlocks.get(teacherIndex).addAll(blocks);
                next = teacherIndex + 1;

                sectionService.createSection(new SectionCreateDTO(
                        subject.getId(), periodId, teacherIds.get(teacherIndex), SECTION_CAPACITY, blocks));
                created++;
            }
        }
        return created;
    }

    // slot 0 = lunes módulo 1, 1 = lunes módulo 2, ... día por día
    private static ScheduleBlockDTO blockAt(int slot) {
        int wrapped = slot % (DAYS * MODULES_PER_DAY);
        return new ScheduleBlockDTO(WeekDay.values()[wrapped / MODULES_PER_DAY], wrapped % MODULES_PER_DAY + 1);
    }

    private static int pickFreeTeacher(List<Set<ScheduleBlockDTO>> busyBlocks, List<ScheduleBlockDTO> blocks, int start) {
        for (int i = 0; i < busyBlocks.size(); i++) {
            int candidate = (start + i) % busyBlocks.size();
            boolean free = true;
            for (ScheduleBlockDTO block : blocks) {
                if (busyBlocks.get(candidate).contains(block)) {
                    free = false;
                    break;
                }
            }
            if (free) {
                return candidate;
            }
        }
        throw new IllegalStateException("Ningún docente de demo tiene libres los bloques de la sección");
    }

    private Career findCareer(String code) {
        for (Career career : careerRepository.findAll()) {
            if (career.getCode().equals(code)) {
                return career;
            }
        }
        throw new IllegalStateException("Carrera de demo no encontrada: " + code);
    }
}
