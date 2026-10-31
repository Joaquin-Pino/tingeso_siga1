package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectUpdateDTO;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.SubjectPrerequisite;
import cl.joaquin.siga.Entities.University.WeeklyHours;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private static final int MAX_PREREQUISITES = 3;
    private static final int MAX_WEEKLY_HOURS_EXCLUSIVE = 8;
    private static final int MIN_SEMESTER = 1;
    private static final int MAX_SEMESTER = 4;
    private static final int MIN_CREDITS = 1;
    private static final int MAX_CREDITS = 7;

    private final SubjectRepository subjectRepository;
    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;

    public SubjectResponseDTO getById(Long subjectId) {
        Subject subject = findSubject(subjectId);
        return toResponseDTO(subject, findPrerequisiteIds(subjectId));
    }

    public List<SubjectResponseDTO> getAllStudyPlanSubjects(Long studyPlanId) {
        if (!studyPlanRepository.existsById(studyPlanId)) {
            throw new NotFoundException("Plan de estudios no encontrado: " + studyPlanId);
        }

        List<SubjectResponseDTO> result = new ArrayList<>();
        for (Subject subject : subjectRepository.findByStudyPlanId(studyPlanId)) {
            result.add(toResponseDTO(subject, findPrerequisiteIds(subject.getId())));
        }
        return result;
    }

    @Transactional
    public SubjectResponseDTO createSubject(SubjectCreateDTO dto) {
        validateSemesterAndCredits(dto.semester(), dto.credits());
        if (!studyPlanRepository.existsById(dto.studyPlanId())) {
            throw new NotFoundException("Plan de estudios no encontrado: " + dto.studyPlanId());
        }
        if (subjectRepository.existsByStudyPlanIdAndCode(dto.studyPlanId(), dto.code())) {
            throw new IllegalArgumentException(
                    "Ya existe una asignatura con el código " + dto.code() + " en el plan: " + dto.studyPlanId());
        }

        // si hay estudiantes con este plan, no se le pueden agregar más asignaturas
        if (studentRepository.existsByStudyPlanId(dto.studyPlanId())) {
            throw new IllegalStateException(
                    "No se pueden agregar asignaturas: hay estudiantes inscritos en el plan: " + dto.studyPlanId());
        }

        validateWeeklyHours(dto.theoryHours(), dto.exerciseHours(), dto.labHours());
        Set<Long> prerequisiteIds = nullToEmpty(dto.prerequisiteIds());
        validatePrerequisites(null, dto.studyPlanId(), dto.semester(), prerequisiteIds);

        Subject subject = new Subject();
        subject.setStudyPlanId(dto.studyPlanId());
        subject.setCode(dto.code());
        subject.setName(dto.name());
        subject.setSemester(dto.semester());
        subject.setWeeklyHours(new WeeklyHours(dto.theoryHours(), dto.exerciseHours(), dto.labHours()));
        subject.setCredits(dto.credits());
        Subject saved = subjectRepository.save(subject);

        savePrerequisites(saved.getId(), prerequisiteIds);
        return toResponseDTO(saved, List.copyOf(prerequisiteIds));
    }

    // El historial académico guarda un snapshot de la asignatura, así que editarla no lo altera
    @Transactional
    public SubjectResponseDTO updateSubject(Long subjectId, SubjectUpdateDTO dto) {
        validateSemesterAndCredits(dto.semester(), dto.credits());
        Subject subject = findSubject(subjectId);

        // el código debe seguir siendo único dentro del plan
        if (!dto.code().equals(subject.getCode())
                && subjectRepository.existsByStudyPlanIdAndCode(subject.getStudyPlanId(), dto.code())) {
            throw new IllegalArgumentException(
                    "Ya existe una asignatura con el código " + dto.code() + " en el plan: " + subject.getStudyPlanId());
        }
        validateWeeklyHours(dto.theoryHours(), dto.exerciseHours(), dto.labHours());
        Set<Long> prerequisiteIds = nullToEmpty(dto.prerequisiteIds());
        validatePrerequisites(subjectId, subject.getStudyPlanId(), dto.semester(), prerequisiteIds);
        validateDependentsStillLater(subjectId, dto.semester());

        subject.setCode(dto.code());
        subject.setName(dto.name());
        subject.setSemester(dto.semester());
        subject.setWeeklyHours(new WeeklyHours(dto.theoryHours(), dto.exerciseHours(), dto.labHours()));
        subject.setCredits(dto.credits());
        Subject saved = subjectRepository.save(subject);

        // se borran los prerrequisitos anteriores y se guardan los nuevos. El flush manda el borrado a la BD
        // antes de insertar; sin él, reinsertar un prerrequisito que ya estaba chocaría con el unique
        subjectPrerequisiteRepository.deleteBySubjectId(subjectId);
        subjectPrerequisiteRepository.flush();
        savePrerequisites(subjectId, prerequisiteIds);
        return toResponseDTO(saved, List.copyOf(prerequisiteIds));
    }

    @Transactional
    public void deleteSubject(Long subjectId) {
        Subject subject = findSubject(subjectId);

        if (academicRecordRepository.existsBySubjectId(subjectId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: la asignatura forma parte del historial académico de estudiantes: " + subjectId);
        }
        // los prerrequisitos son IDs sin FK, así que borrarla dejaría a otras asignaturas apuntando a nada
        if (subjectPrerequisiteRepository.existsByPrerequisiteSubjectId(subjectId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: la asignatura es prerrequisito de otras asignaturas: " + subjectId);
        }
        if (sectionRepository.existsBySubjectId(subjectId)
                || courseRegistrationRepository.existsBySubjectId(subjectId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: la asignatura tiene secciones o inscripciones asociadas: " + subjectId);
        }

        subjectPrerequisiteRepository.deleteBySubjectId(subjectId);
        subjectRepository.delete(subject);
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + subjectId));
    }

    private static void validateSemesterAndCredits(int semester, int credits) {
        if (semester < MIN_SEMESTER || semester > MAX_SEMESTER) {
            throw new IllegalArgumentException(
                    "El semestre debe estar entre " + MIN_SEMESTER + " y " + MAX_SEMESTER);
        }
        if (credits < MIN_CREDITS || credits > MAX_CREDITS) {
            throw new IllegalArgumentException("Los SCT deben estar entre " + MIN_CREDITS + " y " + MAX_CREDITS);
        }
    }

    private static void validateWeeklyHours(int theory, int exercise, int lab) {
        if (theory < 0 || exercise < 0 || lab < 0) {
            throw new IllegalArgumentException("Las horas semanales no pueden ser negativas");
        }
        if (theory + exercise + lab >= MAX_WEEKLY_HOURS_EXCLUSIVE) {
            throw new IllegalArgumentException(
                    "La suma de horas semanales (teoría + ejercicios + laboratorio) debe ser menor a "
                            + MAX_WEEKLY_HOURS_EXCLUSIVE);
        }
    }

    // subjectId es null al crear (la asignatura aún no existe y no puede ser su propio prerrequisito)
    private void validatePrerequisites(Long subjectId, Long studyPlanId, int semester, Set<Long> prerequisiteIds) {
        if (prerequisiteIds.size() > MAX_PREREQUISITES) {
            throw new IllegalArgumentException(
                    "Una asignatura puede tener como máximo " + MAX_PREREQUISITES + " prerrequisitos");
        }
        if (subjectId != null && prerequisiteIds.contains(subjectId)) {
            throw new IllegalArgumentException("Una asignatura no puede ser prerrequisito de sí misma");
        }

        for (Long prerequisiteId : prerequisiteIds) {
            Subject prerequisite = subjectRepository.findById(prerequisiteId)
                    .orElseThrow(() -> new NotFoundException("Prerrequisito no encontrado: " + prerequisiteId));

            if (!prerequisite.getStudyPlanId().equals(studyPlanId)) {
                throw new IllegalArgumentException(
                        "El prerrequisito " + prerequisite.getCode() + " no pertenece al mismo plan de estudios");
            }
            if (prerequisite.getSemester() >= semester) {
                throw new IllegalArgumentException(
                        "El prerrequisito " + prerequisite.getCode() + " debe ser de un semestre anterior al "
                                + semester);
            }
        }
    }

    // al cambiar el semestre, las asignaturas que dependen de esta deben seguir estando en un semestre posterior
    private void validateDependentsStillLater(Long subjectId, int newSemester) {
        for (SubjectPrerequisite row : subjectPrerequisiteRepository.findByPrerequisiteSubjectId(subjectId)) {
            Subject dependent = findSubject(row.getSubjectId());
            if (dependent.getSemester() <= newSemester) {
                throw new IllegalArgumentException(
                        "No se puede mover la asignatura al semestre " + newSemester + ": es prerrequisito de "
                                + dependent.getCode() + " (semestre " + dependent.getSemester() + ")");
            }
        }
    }

    private List<Long> findPrerequisiteIds(Long subjectId) {
        List<Long> prerequisiteIds = new ArrayList<>();
        for (SubjectPrerequisite row : subjectPrerequisiteRepository.findBySubjectId(subjectId)) {
            prerequisiteIds.add(row.getPrerequisiteSubjectId());
        }
        return prerequisiteIds;
    }

    private void savePrerequisites(Long subjectId, Set<Long> prerequisiteIds) {
        for (Long prerequisiteId : prerequisiteIds) {
            SubjectPrerequisite row = new SubjectPrerequisite();
            row.setSubjectId(subjectId);
            row.setPrerequisiteSubjectId(prerequisiteId);
            subjectPrerequisiteRepository.save(row);
        }
    }

    private static Set<Long> nullToEmpty(Set<Long> ids) {
        return ids == null ? Set.of() : ids;
    }

    private static SubjectResponseDTO toResponseDTO(Subject subject, List<Long> prerequisiteIds) {
        WeeklyHours hours = subject.getWeeklyHours();
        return new SubjectResponseDTO(
                subject.getId(),
                subject.getStudyPlanId(),
                subject.getCode(),
                subject.getName(),
                subject.getSemester(),
                hours.getTheoryHours(),
                hours.getExerciseHours(),
                hours.getLabHours(),
                subject.getCredits(),
                prerequisiteIds
        );
    }
}
