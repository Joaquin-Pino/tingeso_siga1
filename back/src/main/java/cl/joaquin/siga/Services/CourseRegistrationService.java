package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.SectionScheduleBlock;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.SubjectPrerequisite;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SectionScheduleBlockRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseRegistrationService {
    private static final int MIN_SUBJECTS = 3;
    private static final int MAX_SUBJECTS = 6;

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final SectionScheduleBlockRepository sectionScheduleBlockRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final PeriodEnrollmentRepository periodEnrollmentRepository;
    private final Clock clock;

    // periodId opcional: sin él devuelve todas las inscripciones del estudiante
    public List<CourseRegistrationResponseDTO> getByStudent(Long studentId, Long periodId) {
        findStudent(studentId);
        List<CourseRegistration> registrations = periodId == null
                ? courseRegistrationRepository.findByStudentId(studentId)
                : courseRegistrationRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId);
        return toResponseDTOs(registrations);
    }

    // inscribe un lote de secciones de un mismo período. Todo o nada: los límites 3-6, el tope de semestre y
    // los horarios son reglas sobre el conjunto, no sobre cada sección. Cuenta también lo ya inscrito en el período
    @Transactional
    public List<CourseRegistrationResponseDTO> register(Long studentId, List<Long> sectionIds) {
        if (Set.copyOf(sectionIds).size() != sectionIds.size()) {
            throw new IllegalArgumentException("El lote tiene secciones repetidas");
        }
        Student student = findStudent(studentId);
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new IllegalStateException("Solo los estudiantes regulares pueden inscribir: " + studentId);
        }

        List<Section> sections = findSections(sectionIds);
        Long periodId = periodOf(sections);
        validatePeriodOpen(periodId);
        if (!periodEnrollmentRepository.existsByStudentIdAndAcademicPeriodId(studentId, periodId)) {
            throw new IllegalStateException(
                    "El estudiante " + studentId + " no está matriculado en el período: " + periodId);
        }

        Map<Long, Subject> planSubjects = new HashMap<>();
        for (Subject subject : subjectRepository.findByStudyPlanId(student.getStudyPlanId())) {
            planSubjects.put(subject.getId(), subject);
        }
        // comprobacion de prerequisitos
        Set<Long> approvedIds = approvedSubjectIds(studentId);
        Map<Long, Set<Long>> prerequisites = prerequisitesBySubject(planSubjects.keySet());
        List<CourseRegistration> existing = courseRegistrationRepository
                .findByStudentIdAndAcademicPeriodId(studentId, periodId);

        validateSubjects(sections, existing, planSubjects, approvedIds, prerequisites);
        validateLoadAndSemester(sections, existing, planSubjects, approvedIds, prerequisites, periodId);
        validateNoScheduleConflict(sections, existing);

        // los cupos se toman al final y en orden de id: así las filas de sección se bloquean poco tiempo
        // y dos lotes cruzados no se bloquean entre sí. Si algo falla se hace rollback de todo
        List<Section> sectionsById = new ArrayList<>(sections);
        sectionsById.sort(Comparator.comparing(Section::getId));
        for (Section section : sectionsById) {
            if (sectionRepository.reserveSeat(section.getId()) == 0) {
                throw new IllegalStateException("La sección no tiene cupo: " + section.getId());
            }
        }

        // guardamos todos los registros hechos
        LocalDate today = LocalDate.now(clock);
        List<CourseRegistration> created = new ArrayList<>();
        for (Section section : sections) {
            CourseRegistration registration = new CourseRegistration();
            registration.setStudentId(studentId);
            registration.setSectionId(section.getId());
            registration.setSubjectId(section.getSubjectId());
            registration.setAcademicPeriodId(periodId);
            registration.setRegistrationDate(today);
            created.add(courseRegistrationRepository.save(registration));
        }
        return toResponseDTOs(created);
    }

    // la anulación borra la fila y libera el cupo; el estudiante debe quedar con al menos 3 asignaturas
    @Transactional
    public void cancel(Long studentId, Long registrationId) {
        findStudent(studentId);
        CourseRegistration registration = courseRegistrationRepository.findById(registrationId)
                .filter(r -> r.getStudentId().equals(studentId))
                .orElseThrow(() -> new NotFoundException(
                        "Inscripción " + registrationId + " no encontrada para el estudiante: " + studentId));
        validatePeriodOpen(registration.getAcademicPeriodId());
        // una inscripción con nota no se anula: la nota se perdería junto con la fila
        if (registration.getGrade() != null) {
            throw new IllegalStateException("No se puede anular: la inscripción ya tiene nota: " + registrationId);
        }

        int current = courseRegistrationRepository
                .findByStudentIdAndAcademicPeriodId(studentId, registration.getAcademicPeriodId()).size();
        if (current - 1 < MIN_SUBJECTS) {
            throw new IllegalStateException(
                    "No se puede anular: el estudiante debe mantener al menos " + MIN_SUBJECTS + " asignaturas");
        }

        courseRegistrationRepository.delete(registration);
        if (sectionRepository.releaseSeat(registration.getSectionId()) == 0) {
            throw new IllegalStateException(
                    "El contador de inscritos de la sección ya estaba en 0: " + registration.getSectionId());
        }
    }

    // cada asignatura: del plan del estudiante, no aprobada, no inscrita ya en el período y con prerrequisitos aprobados
    private static void validateSubjects(List<Section> sections, List<CourseRegistration> existing,
                                         Map<Long, Subject> planSubjects, Set<Long> approvedIds,
                                         Map<Long, Set<Long>> prerequisites) {
        Set<Long> alreadyRegistered = subjectIdsOf(existing);
        for (Section section : sections) {
            Long subjectId = section.getSubjectId();
            Subject subject = planSubjects.get(subjectId);
            if (subject == null) {
                throw new IllegalStateException(
                        "La asignatura " + subjectId + " no pertenece al plan de estudios del estudiante");
            }
            if (approvedIds.contains(subjectId)) {
                throw new IllegalStateException("La asignatura " + subject.getCode() + " ya está aprobada");
            }
            if (alreadyRegistered.contains(subjectId)) {
                throw new IllegalStateException(
                        "La asignatura " + subject.getCode() + " ya está inscrita en el período");
            }
            if (!approvedIds.containsAll(prerequisites.getOrDefault(subjectId, Set.of()))) {
                throw new IllegalStateException(
                        "La asignatura " + subject.getCode() + " tiene prerrequisitos sin aprobar");
            }
        }
    }

    // entre 3 y 6 asignaturas (si quedan menos de 3 disponibles, basta con inscribirlas todas) y ninguna de un
    // semestre superior mientras quede una disponible de semestre inferior sin inscribir
    private void validateLoadAndSemester(List<Section> sections, List<CourseRegistration> existing,
                                         Map<Long, Subject> planSubjects, Set<Long> approvedIds,
                                         Map<Long, Set<Long>> prerequisites, Long periodId) {
        Set<Long> registeredIds = subjectIdsOf(existing);
        for (Section section : sections) {
            registeredIds.add(section.getSubjectId());
        }

        if (registeredIds.size() > MAX_SUBJECTS) {
            throw new IllegalStateException("No se pueden inscribir más de " + MAX_SUBJECTS + " asignaturas por período");
        }

        // pendientes disponibles: se ofrecen en el período, no están aprobadas y sus prerrequisitos sí
        Set<Long> offeredIds = new HashSet<>();
        for (Section offered : sectionRepository.findByAcademicPeriodId(periodId)) {
            offeredIds.add(offered.getSubjectId());
        }
        List<Subject> available = new ArrayList<>();
        for (Subject subject : planSubjects.values()) {
            boolean offeredAndPending = offeredIds.contains(subject.getId()) && !approvedIds.contains(subject.getId());
            if (offeredAndPending && approvedIds.containsAll(prerequisites.getOrDefault(subject.getId(), Set.of()))) {
                available.add(subject);
            }
        }

        int required = Math.min(MIN_SUBJECTS, available.size());
        if (registeredIds.size() < required) {
            throw new IllegalStateException(
                    "Se deben inscribir al menos " + required + " asignaturas por período");
        }

        for (Section section : sections) {
            Subject subject = planSubjects.get(section.getSubjectId());
            for (Subject pending : available) {
                if (pending.getSemester() < subject.getSemester() && !registeredIds.contains(pending.getId())) {
                    throw new IllegalStateException(
                            "No se puede inscribir " + subject.getCode() + " (semestre " + subject.getSemester()
                                    + ") mientras haya pendientes disponibles de un semestre inferior: "
                                    + pending.getCode());
                }
            }
        }
    }

    // ningún día + módulo repetido entre las secciones nuevas y las ya inscritas
    private void validateNoScheduleConflict(List<Section> sections, List<CourseRegistration> existing) {
        Set<Long> allSectionIds = new HashSet<>();
        for (Section section : sections) {
            allSectionIds.add(section.getId());
        }
        for (CourseRegistration registration : existing) {
            allSectionIds.add(registration.getSectionId());
        }

        Map<ScheduleBlockDTO, Long> owners = new HashMap<>();
        for (SectionScheduleBlock row : sectionScheduleBlockRepository.findBySectionIdIn(allSectionIds)) {
            ScheduleBlockDTO block = new ScheduleBlockDTO(row.getDay(), row.getModule());
            Long other = owners.put(block, row.getSectionId());
            if (other != null && !other.equals(row.getSectionId())) {
                throw new IllegalStateException(
                        "Conflicto de horario entre las secciones " + other + " y " + row.getSectionId()
                                + " en el bloque " + block.day() + "-" + block.module());
            }
        }
    }

    // solo las aprobadas del historial: una inscripción del período en curso aún no tiene resultado
    private Set<Long> approvedSubjectIds(Long studentId) {
        Set<Long> approvedIds = new HashSet<>();
        for (AcademicRecord record : academicRecordRepository.findByStudentIdAndResult(studentId, CourseResult.PASSED)) {
            approvedIds.add(record.getSubjectId());
        }
        return approvedIds;
    }

    private Map<Long, Set<Long>> prerequisitesBySubject(Set<Long> subjectIds) {
        Map<Long, Set<Long>> result = new HashMap<>();
        for (SubjectPrerequisite row : subjectPrerequisiteRepository.findBySubjectIdIn(subjectIds)) {
            result.computeIfAbsent(row.getSubjectId(), k -> new HashSet<>()).add(row.getPrerequisiteSubjectId());
        }
        return result;
    }

    private List<Section> findSections(List<Long> sectionIds) {
        List<Section> sections = sectionRepository.findAllById(sectionIds);
        Set<Long> foundIds = new HashSet<>();
        for (Section section : sections) {
            foundIds.add(section.getId());
        }
        for (Long id : sectionIds) {
            if (!foundIds.contains(id)) {
                throw new NotFoundException("Sección no encontrada: " + id);
            }
        }
        return sections;
    }

    private static Long periodOf(List<Section> sections) {
        Set<Long> periodIds = new HashSet<>();
        for (Section section : sections) {
            periodIds.add(section.getAcademicPeriodId());
        }
        if (periodIds.size() != 1) {
            throw new IllegalArgumentException("Todas las secciones del lote deben ser del mismo período");
        }
        return periodIds.iterator().next();
    }

    private void validatePeriodOpen(Long periodId) {
        AcademicPeriod period = academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Período académico no encontrado: " + periodId));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("El período académico está cerrado: " + periodId);
        }
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));
    }

    private static Set<Long> subjectIdsOf(List<CourseRegistration> registrations) {
        Set<Long> subjectIds = new HashSet<>();
        for (CourseRegistration registration : registrations) {
            subjectIds.add(registration.getSubjectId());
        }
        return subjectIds;
    }

    private static List<CourseRegistrationResponseDTO> toResponseDTOs(List<CourseRegistration> registrations) {
        List<CourseRegistrationResponseDTO> result = new ArrayList<>();
        for (CourseRegistration registration : registrations) {
            result.add(toResponseDTO(registration));
        }
        return result;
    }

    static CourseRegistrationResponseDTO toResponseDTO(CourseRegistration registration) {
        return new CourseRegistrationResponseDTO(
                registration.getId(),
                registration.getStudentId(),
                registration.getSectionId(),
                registration.getSubjectId(),
                registration.getAcademicPeriodId(),
                registration.getRegistrationDate(),
                registration.getGrade(),
                registration.getResult()
        );
    }
}
