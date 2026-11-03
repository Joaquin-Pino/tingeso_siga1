package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.GradeUpdateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherStatusUpdateDTO;
import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.People.TeacherStatus;
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
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Datos de demo para mostrar todas las funciones: docentes, estudiantes en distintas situaciones y 4 períodos
// (2025-1, 2025-2 y 2026-1 ya cerrados, con su historial académico, y 2026-2 abierto). Todo pasa por los mismos
// Services que la API (matrícula, inscripción, notas, cierre, cuentas de Keycloak), así que las reglas se cumplen
// de verdad y necesita el catálogo del SeedDataService ya cargado.
//
// Estudiantes (todas las contraseñas son TEMPORARY_PASSWORD):
//  1 TAP  ingresó en 2025-1 y va al día; en 2026-2 cursa 4º semestre aprobando todo. Al cerrar el período egresa
//  2 TAP  aprobó 1º y 2º y del 3º todo menos TAP306 (3,3); matriculado en 2026-2 pero sin inscribir. Para la inscripción en
//         vivo: TAP306 se ofrece en 2026-2 con un bloque en común con TAP404 (tope de horario), y mientras TAP306 siga
//         pendiente no puede inscribir 4º semestre sin ella. No se puede inscribir TAP306 junto con TAP404
//  3 TAD  reprobó TAD202 en 2025-2 y la repitió en 2026-1; en 2026-2 mezcla 3º y 4º semestre (historial con FAILED y PASSED)
//  4 TCS  al día, matriculado en 2026-2 pero sin inscribir: sirve para mostrar la inscripción en vivo (6 asignaturas de 4º)
//         y la anulación. Si se inscribe, sus notas quedan pendientes y los docentes deben ponerlas antes de cerrar
//  5 TAP  ingresó en 2026-1 y reprobó TAP101; la repite en 2026-2 con 3,0: la reprueba por segunda vez y queda ELIMINATED
//  6 TCS  ingresó en 2026-1; en 2026-2 cursa 4 asignaturas de 2º semestre
//  7 TAD  ingresó en 2026-2 con 4 asignaturas de 1º semestre
// El docente 6 está INACTIVE y sin secciones. Todas las inscripciones del período 2026-2 ya tienen nota, así que se puede
// cerrar de inmediato. No es una sola transacción porque las cuentas de Keycloak no se pueden revertir
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    // contraseña temporal de todas las cuentas de demo; Keycloak pide cambiarla en el primer ingreso
    static final String TEMPORARY_PASSWORD = "Siga2026!";
    private static final int SECTION_CAPACITY = 30;
    private static final int DAYS = WeekDay.values().length;
    private static final int MODULES_PER_DAY = 6;
    private static final int HOURS_PER_BLOCK = 2;

    private record TeacherSeed(String run, String fullName, String email, String title, AcademicDegree degree,
                               boolean active) {
    }

    // grade null = nota automática aprobatoria
    private record Take(String code, String grade) {
    }

    // periods tiene un elemento por período de PERIODS: null = no matriculado, lista vacía = matriculado sin inscribir
    private record StudentSeed(String run, String fullName, String email, String careerCode, List<List<Take>> periods) {
    }

    // asignatura pendiente de semestre inferior que en el último período se ofrece con un bloque en común con otra
    // (tope de horario) para mostrar esa regla en la inscripción
    private record ClashSeed(String careerCode, String pendingCode, String clashWithCode) {
    }

    private static final ClashSeed CLASH = new ClashSeed("10450", "TAP306", "TAP404");

    private record PeriodSeed(int year, Semester semester, int coreSemester, boolean close) {
    }

    private static final List<PeriodSeed> PERIODS = List.of(
            new PeriodSeed(2025, Semester.FIRST, 1, true),
            new PeriodSeed(2025, Semester.SECOND, 2, true),
            new PeriodSeed(2026, Semester.FIRST, 3, true),
            new PeriodSeed(2026, Semester.SECOND, 4, false));

    private static final List<TeacherSeed> TEACHERS = List.of(
            new TeacherSeed("10.111.111-1", "Profesor 1", "profesor1@ipt.cl",
                    "Ingeniera Civil Informática", AcademicDegree.MASTER, true),
            new TeacherSeed("10.222.222-2", "Profesor 2", "profesor2@ipt.cl",
                    "Ingeniero en Computación", AcademicDegree.BACHELOR, true),
            new TeacherSeed("10.333.333-3", "Profesor 3", "profesor3@ipt.cl",
                    "Ingeniera Civil en Informática", AcademicDegree.DOCTORATE, true),
            new TeacherSeed("10.444.444-4", "Profesor 4", "profesor4@ipt.cl",
                    "Ingeniero Civil en Telecomunicaciones", AcademicDegree.MASTER, true),
            new TeacherSeed("10.555.555-5", "Profesor 5", "profesor5@ipt.cl",
                    "Ingeniera Estadística", AcademicDegree.DOCTORATE, true),
            new TeacherSeed("10.666.666-6", "Profesor 6", "profesor6@ipt.cl",
                    "Ingeniero Civil Industrial", AcademicDegree.BACHELOR, false));

    private static final List<StudentSeed> STUDENTS = List.of(
            new StudentSeed("20.111.111-1", "Estudiante 1", "estudiante1@ipt.cl", "10450", periods(
                    passAll("TAP", 1), passAll("TAP", 2), passAll("TAP", 3),
                    passAll("TAP", 4))),
            new StudentSeed("20.222.222-2", "Estudiante 2", "estudiante2@ipt.cl", "10450", periods(
                    passAll("TAP", 1), passAll("TAP", 2), override(passAll("TAP", 3), grade("TAP306", "3.3")),
                    List.of())),
            new StudentSeed("20.333.333-3", "Estudiante 3", "estudiante3@ipt.cl", "10451", periods(
                    passAll("TAD", 1),
                    override(passAll("TAD", 2), grade("TAD202", "3.4")),
                    List.of(grade("TAD202", "4.8"), pass("TAD303"), pass("TAD304"), pass("TAD305"), pass("TAD306")),
                    List.of(pass("TAD301"), pass("TAD302"), pass("TAD402"), pass("TAD406")))),
            new StudentSeed("20.444.444-4", "Estudiante 4", "estudiante4@ipt.cl", "10452", periods(
                    passAll("TCS", 1), passAll("TCS", 2), passAll("TCS", 3), List.of())),
            new StudentSeed("20.555.555-5", "Estudiante 5", "estudiante5@ipt.cl", "10450", periods(
                    null, null,
                    override(passAll("TAP", 1), grade("TAP101", "3.1")),
                    List.of(grade("TAP101", "3.0"), pass("TAP203"), pass("TAP205"), pass("TAP206")))),
            new StudentSeed("20.666.666-6", "Estudiante 6", "estudiante6@ipt.cl", "10452", periods(
                    null, null, passAll("TCS", 1),
                    List.of(pass("TCS201"), pass("TCS204"), pass("TCS205"), pass("TCS206")))),
            new StudentSeed("20.777.777-7", "Estudiante 7", "estudiante7@ipt.cl", "10451", periods(
                    null, null, null,
                    List.of(pass("TAD101"), pass("TAD102"), pass("TAD103"), pass("TAD104")))));

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final AcademicPeriodService academicPeriodService;
    private final SectionService sectionService;
    private final PeriodEnrollmentService periodEnrollmentService;
    private final CourseRegistrationService courseRegistrationService;
    private final GradeService gradeService;
    private final PeriodCloseService periodCloseService;

    // solo carga si no hay docentes, estudiantes ni períodos (no mezcla con datos reales); devuelve false si no hizo nada
    public boolean seedIfEmpty() {
        if (teacherRepository.count() > 0 || studentRepository.count() > 0) {
            return false;
        }
        if (careerRepository.count() == 0) {
            log.warn("Datos de demo omitidos: falta el catálogo (carreras y asignaturas)");
            return false;
        }
        if (academicPeriodRepository.count() > 0) {
            log.warn("Datos de demo omitidos: ya existen períodos académicos");
            return false;
        }

        Map<String, List<Subject>> catalog = loadCatalog();
        List<Long> teacherIds = seedTeachers();
        List<Long> studentIds = seedStudents();
        for (int index = 0; index < PERIODS.size(); index++) {
            runPeriod(index, catalog, teacherIds, studentIds);
        }
        log.info("Datos de demo: {} docentes activos, {} estudiantes, {} períodos (el último abierto)",
                teacherIds.size(), studentIds.size(), PERIODS.size());
        return true;
    }

    // asignaturas del plan vigente de cada carrera, por código de carrera y ordenadas por código
    private Map<String, List<Subject>> loadCatalog() {
        Map<String, List<Subject>> catalog = new TreeMap<>();
        for (Career career : careerRepository.findAll()) {
            StudyPlan plan = studyPlanRepository.findByCareerIdAndStatus(career.getId(), StudyPlanStatus.CURRENT)
                    .orElseThrow(() -> new IllegalStateException("La carrera no tiene plan vigente: " + career.getCode()));
            List<Subject> subjects = new ArrayList<>(subjectRepository.findByStudyPlanId(plan.getId()));
            subjects.sort(Comparator.comparing(Subject::getCode));
            catalog.put(career.getCode(), subjects);
        }
        return catalog;
    }

    // devuelve solo los docentes activos (los que reciben secciones)
    private List<Long> seedTeachers() {
        List<Long> activeIds = new ArrayList<>();
        for (TeacherSeed teacher : TEACHERS) {
            Long id = teacherService.saveTeacher(new TeacherCreateDTO(TEMPORARY_PASSWORD, teacher.run(),
                    teacher.fullName(), teacher.email(), teacher.title(), teacher.degree())).id();
            if (teacher.active()) {
                activeIds.add(id);
            } else {
                teacherService.updateStatus(id, new TeacherStatusUpdateDTO(TeacherStatus.INACTIVE));
            }
        }
        return activeIds;
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

    // un período completo: lo crea, ofrece las secciones, matricula, inscribe, pone las notas y (si no es el último) lo cierra
    private void runPeriod(int index, Map<String, List<Subject>> catalog, List<Long> teacherIds,
                           List<Long> studentIds) {
        PeriodSeed seed = PERIODS.get(index);
        Long periodId = academicPeriodService.createPeriod(
                new AcademicPeriodCreateDTO(seed.year(), seed.semester())).id();
        Map<String, SectionResponseDTO> sections = offerSections(index, periodId, catalog, teacherIds);

        for (int i = 0; i < STUDENTS.size(); i++) {
            if (STUDENTS.get(i).periods().get(index) != null) {
                periodEnrollmentService.enroll(periodId, new PeriodEnrollmentCreateDTO(studentIds.get(i)));
            }
        }
        for (int i = 0; i < STUDENTS.size(); i++) {
            List<Take> takes = STUDENTS.get(i).periods().get(index);
            if (takes != null && !takes.isEmpty()) {
                registerAndGrade(i, studentIds.get(i), takes, sections);
            }
        }
        if (seed.close()) {
            periodCloseService.closePeriod(periodId);
        }
    }

    // oferta del período: todas las asignaturas del semestre que toca en cada carrera, más las de otros semestres que
    // algún estudiante necesita (repeticiones, ingresos nuevos). Las que un mismo estudiante cursa juntas no comparten
    // bloque, y cada docente se elige entre los que tienen libres esos bloques en el período
    private Map<String, SectionResponseDTO> offerSections(int index, Long periodId, Map<String, List<Subject>> catalog,
                                                          List<Long> teacherIds) {
        Map<String, SectionResponseDTO> sections = new LinkedHashMap<>();
        List<Set<Integer>> busySlots = new ArrayList<>(); // slots ocupados de cada docente en el período
        for (int i = 0; i < teacherIds.size(); i++) {
            busySlots.add(new HashSet<>());
        }
        int next = 0; // el docente por el que parte la búsqueda rota para repartir la carga

        for (Map.Entry<String, List<Subject>> career : catalog.entrySet()) {
            List<Subject> offered = new ArrayList<>();
            List<Set<String>> groups = new ArrayList<>(); // conjuntos de asignaturas que se pueden cursar juntas
            Set<String> core = new HashSet<>();
            for (Subject subject : career.getValue()) {
                if (subject.getSemester() == PERIODS.get(index).coreSemester()) {
                    offered.add(subject);
                    core.add(subject.getCode());
                }
            }
            groups.add(core);
            boolean clashPeriod = index == PERIODS.size() - 1 && career.getKey().equals(CLASH.careerCode());
            for (StudentSeed student : STUDENTS) {
                List<Take> takes = student.periods().get(index);
                if (!student.careerCode().equals(career.getKey()) || takes == null) {
                    continue;
                }
                Set<String> codes = new HashSet<>();
                for (Take take : takes) {
                    codes.add(take.code());
                }
                groups.add(codes);
                for (Subject subject : career.getValue()) {
                    if (codes.contains(subject.getCode()) && !offered.contains(subject)) {
                        offered.add(subject);
                    }
                }
            }

            if (clashPeriod) {
                for (Subject subject : career.getValue()) {
                    if (subject.getCode().equals(CLASH.pendingCode()) && !offered.contains(subject)) {
                        offered.add(subject); // al final: su tope se calcula con el horario ya asignado
                    }
                }
            }

            Map<String, List<Integer>> slotsByCode = new HashMap<>();
            for (Subject subject : offered) {
                Set<Integer> forbidden = slotsOfNeighbors(subject, groups, slotsByCode);
                int needed = blocksOf(subject);
                int teacherIndex = -1;
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < busySlots.size() && teacherIndex < 0; i++) {
                    int candidate = (next + i) % busySlots.size();
                    slots = clashPeriod && subject.getCode().equals(CLASH.pendingCode())
                            ? clashSlots(needed, slotsByCode, busySlots.get(candidate))
                            : freeSlots(needed, forbidden, busySlots.get(candidate));
                    if (slots.size() == needed) {
                        teacherIndex = candidate;
                    }
                }
                if (teacherIndex < 0) {
                    throw new IllegalStateException("No quedan bloques libres para la asignatura " + subject.getCode());
                }
                busySlots.get(teacherIndex).addAll(slots);
                slotsByCode.put(subject.getCode(), slots);
                next = teacherIndex + 1;

                sections.put(subject.getCode(), sectionService.createSection(new SectionCreateDTO(
                        subject.getId(), periodId, teacherIds.get(teacherIndex), SECTION_CAPACITY,
                        toBlocks(slots))));
            }
        }
        return sections;
    }

    private static int blocksOf(Subject subject) {
        int totalHours = subject.getWeeklyHours().getTheoryHours()
                + subject.getWeeklyHours().getExerciseHours() + subject.getWeeklyHours().getLabHours();
        return totalHours / HOURS_PER_BLOCK;
    }

    // slots ya tomados por las asignaturas con las que esta comparte algún estudiante
    private static Set<Integer> slotsOfNeighbors(Subject subject, List<Set<String>> groups,
                                                 Map<String, List<Integer>> slotsByCode) {
        Set<Integer> forbidden = new HashSet<>();
        for (Map.Entry<String, List<Integer>> other : slotsByCode.entrySet()) {
            if (shareGroup(groups, subject.getCode(), other.getKey())) {
                forbidden.addAll(other.getValue());
            }
        }
        return forbidden;
    }

    // primeros slots que no están en ninguno de los dos conjuntos (slot 0 = lunes módulo 1, 1 = lunes módulo 2, ...
    // día por día); si no alcanzan devuelve menos de los pedidos
    private static List<Integer> freeSlots(int needed, Set<Integer> forbidden, Set<Integer> teacherBusy) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < DAYS * MODULES_PER_DAY && slots.size() < needed; slot++) {
            if (!forbidden.contains(slot) && !teacherBusy.contains(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    // el primer bloque de la asignatura con la que debe chocar y el resto en slots que nadie más usa;
    // si el docente no tiene libre el bloque del choque devuelve una lista vacía
    private static List<Integer> clashSlots(int needed, Map<String, List<Integer>> slotsByCode, Set<Integer> teacherBusy) {
        int clashSlot = slotsByCode.get(CLASH.clashWithCode()).get(0);
        if (teacherBusy.contains(clashSlot)) {
            return new ArrayList<>();
        }
        Set<Integer> taken = new HashSet<>(teacherBusy);
        for (List<Integer> other : slotsByCode.values()) {
            taken.addAll(other);
        }
        List<Integer> slots = new ArrayList<>();
        slots.add(clashSlot);
        slots.addAll(freeSlots(needed - 1, taken, new HashSet<>()));
        return slots;
    }

    private static List<ScheduleBlockDTO> toBlocks(List<Integer> slots) {
        List<ScheduleBlockDTO> blocks = new ArrayList<>();
        for (int slot : slots) {
            blocks.add(new ScheduleBlockDTO(WeekDay.values()[slot / MODULES_PER_DAY], slot % MODULES_PER_DAY + 1));
        }
        return blocks;
    }

    private static boolean shareGroup(List<Set<String>> groups, String code, String otherCode) {
        for (Set<String> group : groups) {
            if (group.contains(code) && group.contains(otherCode)) {
                return true;
            }
        }
        return false;
    }

    // inscribe el lote del estudiante y el docente de cada sección pone la nota
    private void registerAndGrade(int studentIndex, Long studentId, List<Take> takes,
                                  Map<String, SectionResponseDTO> sections) {
        List<Long> sectionIds = new ArrayList<>();
        for (Take take : takes) {
            sectionIds.add(offeredSection(sections, take).id());
        }
        List<CourseRegistrationResponseDTO> registered = courseRegistrationService.register(studentId, sectionIds);

        for (Take take : takes) {
            SectionResponseDTO section = offeredSection(sections, take);
            for (CourseRegistrationResponseDTO registration : registered) {
                if (registration.sectionId().equals(section.id())) {
                    gradeService.registerGrade(section.teacherId(), section.id(), registration.id(),
                            new GradeUpdateDTO(gradeOf(studentIndex, take)));
                }
            }
        }
    }

    private static SectionResponseDTO offeredSection(Map<String, SectionResponseDTO> sections, Take take) {
        SectionResponseDTO section = sections.get(take.code());
        if (section == null) {
            throw new IllegalStateException("La asignatura de demo no se ofreció en el período: " + take.code());
        }
        return section;
    }

    // la nota automática va de 4,5 a 6,8 y depende del estudiante y la asignatura, para que los promedios varíen
    private static BigDecimal gradeOf(int studentIndex, Take take) {
        if (take.grade() != null) {
            return new BigDecimal(take.grade());
        }
        return BigDecimal.valueOf(45 + Math.floorMod(studentIndex * 7 + take.code().hashCode(), 24), 1);
    }

    private static Take pass(String code) {
        return new Take(code, null);
    }

    private static Take grade(String code, String grade) {
        return new Take(code, grade);
    }

    // las 6 asignaturas de un semestre (código = carrera + semestre + 0 + número), todas aprobadas
    private static List<Take> passAll(String prefix, int semester) {
        List<Take> takes = new ArrayList<>();
        for (int number = 1; number <= 6; number++) {
            takes.add(pass(prefix + semester + "0" + number));
        }
        return takes;
    }

    // la lista base con las asignaturas de changes reemplazadas
    private static List<Take> override(List<Take> base, Take... changes) {
        List<Take> result = new ArrayList<>();
        for (Take take : base) {
            Take chosen = take;
            for (Take change : changes) {
                if (change.code().equals(take.code())) {
                    chosen = change;
                }
            }
            result.add(chosen);
        }
        return result;
    }

    // List.of no admite null, y un período sin matrícula se marca con null
    @SafeVarargs
    private static List<List<Take>> periods(List<Take>... periods) {
        List<List<Take>> result = new ArrayList<>();
        for (List<Take> period : periods) {
            result.add(period);
        }
        return result;
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
