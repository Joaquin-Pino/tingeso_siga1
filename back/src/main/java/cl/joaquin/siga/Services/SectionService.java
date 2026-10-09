package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionUpdateDTO;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Entities.People.TeacherStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.SectionScheduleBlock;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.WeeklyHours;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SectionScheduleBlockRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SectionService {
    // cada bloque horario equivale a 2 horas pedagógicas
    private static final int HOURS_PER_BLOCK = 2;

    private final SectionRepository sectionRepository;
    private final SectionScheduleBlockRepository sectionScheduleBlockRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    public SectionResponseDTO getById(Long sectionId) {
        Section section = findSection(sectionId);
        return toResponseDTO(section, findBlocks(sectionId));
    }

    // la oferta académica de un período
    public List<SectionResponseDTO> getAllPeriodSections(Long periodId) {
        if (!academicPeriodRepository.existsById(periodId)) {
            throw new NotFoundException("Período académico no encontrado: " + periodId);
        }
        return toResponseDTOs(sectionRepository.findByAcademicPeriodId(periodId));
    }

    public List<SectionResponseDTO> getAllTeacherSections(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new NotFoundException("Docente no encontrado: " + teacherId);
        }
        return toResponseDTOs(sectionRepository.findByTeacherId(teacherId));
    }

    @Transactional
    public SectionResponseDTO createSection(SectionCreateDTO dto) {
        validatePeriodOpen(dto.academicPeriodId());
        Subject subject = findSubject(dto.subjectId());
        if (sectionRepository.existsBySubjectIdAndAcademicPeriodId(dto.subjectId(), dto.academicPeriodId())) {
            throw new IllegalArgumentException(
                    "Ya existe una sección de la asignatura " + subject.getCode() + " en el período: "
                            + dto.academicPeriodId());
        }
        validateTeacherActive(dto.teacherId());
        validateScheduleBlocks(subject, dto.scheduleBlocks());
        validateNoTeacherConflict(null, dto.teacherId(), dto.academicPeriodId(), dto.scheduleBlocks());

        Section section = new Section();
        section.setSubjectId(dto.subjectId());
        section.setAcademicPeriodId(dto.academicPeriodId());
        section.setTeacherId(dto.teacherId());
        section.setCapacity(dto.capacity());
        section.setEnrolledCount(0); // la sección parte sin inscritos
        Section saved = sectionRepository.save(section);

        saveBlocks(saved.getId(), dto.scheduleBlocks());
        return toResponseDTO(saved, dto.scheduleBlocks());
    }

    @Transactional
    public SectionResponseDTO updateSection(Long sectionId, SectionUpdateDTO dto) {
        Section section = findSection(sectionId);
        validatePeriodOpen(section.getAcademicPeriodId());

        if (dto.capacity() < section.getEnrolledCount()) {
            throw new IllegalStateException(
                    "La capacidad no puede ser menor a los " + section.getEnrolledCount() + " inscritos de la sección");
        }
        // el docente debe estar ACTIVE solo al momento de asignarlo; si no cambia, no se vuelve a exigir
        if (!dto.teacherId().equals(section.getTeacherId())) {
            validateTeacherActive(dto.teacherId());
        }
        validateScheduleBlocks(findSubject(section.getSubjectId()), dto.scheduleBlocks());

        List<ScheduleBlockDTO> currentBlocks = findBlocks(sectionId);
        boolean blocksChanged = !Set.copyOf(currentBlocks).equals(Set.copyOf(dto.scheduleBlocks()));
        // mover el horario podría generar topes con las otras secciones de los estudiantes ya inscritos
        if (blocksChanged && courseRegistrationRepository.existsBySectionId(sectionId)) {
            throw new IllegalStateException(
                    "No se puede cambiar el horario: la sección tiene estudiantes inscritos: " + sectionId);
        }
        validateNoTeacherConflict(sectionId, dto.teacherId(), section.getAcademicPeriodId(), dto.scheduleBlocks());

        section.setTeacherId(dto.teacherId());
        section.setCapacity(dto.capacity());
        Section saved = sectionRepository.save(section);

        if (!blocksChanged) {
            return toResponseDTO(saved, currentBlocks);
        }
        // igual que con los prerrequisitos: el flush manda el borrado antes de insertar, para no chocar
        // con el unique (section_id, day, module) si un bloque se mantiene
        sectionScheduleBlockRepository.deleteBySectionId(sectionId);
        sectionScheduleBlockRepository.flush();
        saveBlocks(sectionId, dto.scheduleBlocks());
        return toResponseDTO(saved, dto.scheduleBlocks());
    }

    // una sección con estudiantes inscritos (o que los tuvo, las inscripciones se conservan) no se elimina
    @Transactional
    public void deleteSection(Long sectionId) {
        Section section = findSection(sectionId);

        if (courseRegistrationRepository.existsBySectionId(sectionId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: la sección tiene estudiantes inscritos: " + sectionId);
        }

        sectionScheduleBlockRepository.deleteBySectionId(sectionId);
        sectionRepository.delete(section);
    }

    private Section findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Sección no encontrada: " + sectionId));
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + subjectId));
    }

    private void validatePeriodOpen(Long periodId) {
        AcademicPeriod period = academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Período académico no encontrado: " + periodId));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("El período académico está cerrado: " + periodId);
        }
    }

    private void validateTeacherActive(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Docente no encontrado: " + teacherId));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new IllegalStateException("Solo se pueden asignar docentes activos: " + teacherId);
        }
    }

    // sin bloques repetidos, y tantos bloques como horas TEL / 2
    private static void validateScheduleBlocks(Subject subject, List<ScheduleBlockDTO> blocks) {
        if (Set.copyOf(blocks).size() != blocks.size()) {
            throw new IllegalArgumentException("La sección tiene bloques horarios duplicados");
        }

        WeeklyHours hours = subject.getWeeklyHours();
        int totalHours = hours.getTheoryHours() + hours.getExerciseHours() + hours.getLabHours();
        if (totalHours % HOURS_PER_BLOCK != 0) {
            throw new IllegalArgumentException(
                    "La asignatura " + subject.getCode() + " tiene " + totalHours
                            + " horas semanales, que no se pueden repartir en bloques de " + HOURS_PER_BLOCK + " horas");
        }
        int requiredBlocks = totalHours / HOURS_PER_BLOCK;
        if (blocks.size() != requiredBlocks) {
            throw new IllegalArgumentException(
                    "La asignatura " + subject.getCode() + " requiere " + requiredBlocks + " bloques horarios ("
                            + totalHours + " horas semanales), pero se indicaron " + blocks.size());
        }
    }

    // el docente no puede tener otra sección del mismo período en el mismo día + módulo.
    // sectionId es null al crear (al editar se excluye la propia sección)
    private void validateNoTeacherConflict(Long sectionId, Long teacherId, Long periodId,
                                           List<ScheduleBlockDTO> blocks) {
        for (Section other : sectionRepository.findByTeacherIdAndAcademicPeriodId(teacherId, periodId)) {
            if (Objects.equals(other.getId(), sectionId)) {
                continue;
            }
            for (ScheduleBlockDTO block : findBlocks(other.getId())) {
                if (blocks.contains(block)) {
                    throw new IllegalStateException(
                            "El docente ya tiene la sección " + other.getId() + " en el bloque "
                                    + block.day() + "-" + block.module());
                }
            }
        }
    }

    private List<ScheduleBlockDTO> findBlocks(Long sectionId) {
        List<ScheduleBlockDTO> blocks = new ArrayList<>();
        for (SectionScheduleBlock row : sectionScheduleBlockRepository.findBySectionId(sectionId)) {
            blocks.add(new ScheduleBlockDTO(row.getDay(), row.getModule()));
        }
        return blocks;
    }

    private void saveBlocks(Long sectionId, List<ScheduleBlockDTO> blocks) {
        for (ScheduleBlockDTO block : blocks) {
            SectionScheduleBlock row = new SectionScheduleBlock();
            row.setSectionId(sectionId);
            row.setDay(block.day());
            row.setModule(block.module());
            sectionScheduleBlockRepository.save(row);
        }
    }

    private List<SectionResponseDTO> toResponseDTOs(List<Section> sections) {
        List<SectionResponseDTO> result = new ArrayList<>();
        for (Section section : sections) {
            result.add(toResponseDTO(section, findBlocks(section.getId())));
        }
        return result;
    }

    private static SectionResponseDTO toResponseDTO(Section section, List<ScheduleBlockDTO> blocks) {
        return new SectionResponseDTO(
                section.getId(),
                section.getSubjectId(),
                section.getAcademicPeriodId(),
                section.getTeacherId(),
                section.getCapacity(),
                section.getEnrolledCount(),
                List.copyOf(blocks)
        );
    }
}
