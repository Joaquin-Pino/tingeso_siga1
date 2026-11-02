package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.GradeUpdateDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.SectionRegistrationResponseDTO;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// registro de notas por el docente responsable de la sección
@Service
@RequiredArgsConstructor
public class GradeService {
    private static final BigDecimal MIN_GRADE = new BigDecimal("1.0");
    private static final BigDecimal MAX_GRADE = new BigDecimal("7.0");


    private final CourseRegistrationRepository courseRegistrationRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    // los inscritos de una sección del docente, con el RUN y nombre de cada estudiante
    public List<SectionRegistrationResponseDTO> getSectionRegistrations(Long teacherId, Long sectionId) {
        findOwnSection(teacherId, sectionId);
        List<CourseRegistration> registrations = courseRegistrationRepository.findBySectionId(sectionId);

        List<Long> studentIds = new ArrayList<>();
        for (CourseRegistration registration : registrations) {
            studentIds.add(registration.getStudentId());
        }
        Map<Long, Student> students = new HashMap<>();
        for (Student student : studentRepository.findAllById(studentIds)) {
            students.put(student.getId(), student);
        }

        List<SectionRegistrationResponseDTO> result = new ArrayList<>();
        for (CourseRegistration registration : registrations) {
            Student student = students.get(registration.getStudentId());
            result.add(new SectionRegistrationResponseDTO(
                    registration.getId(),
                    registration.getStudentId(),
                    student.getRun(),
                    student.getFullName(),
                    registration.getGrade(),
                    registration.getResult()));
        }
        return result;
    }

    // registra o corrige la nota mientras el período esté abierto. El result no se toca: lo calcula el cierre
    @Transactional
    public CourseRegistrationResponseDTO registerGrade(Long teacherId, Long sectionId, Long registrationId,
                                                       GradeUpdateDTO dto) {
        validateGrade(dto.grade());
        Section section = findOwnSection(teacherId, sectionId);

        AcademicPeriod period = academicPeriodRepository.findById(section.getAcademicPeriodId())
                .orElseThrow(() -> new NotFoundException(
                        "Período académico no encontrado: " + section.getAcademicPeriodId()));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("El período académico está cerrado: " + period.getId());
        }

        CourseRegistration registration = courseRegistrationRepository.findById(registrationId)
                .filter(r -> r.getSectionId().equals(sectionId))
                .orElseThrow(() -> new NotFoundException(
                        "Inscripción " + registrationId + " no encontrada en la sección: " + sectionId));

        registration.setGrade(dto.grade());
        return CourseRegistrationService.toResponseDTO(courseRegistrationRepository.save(registration));
    }

    // nota final de 1,0 a 7,0 con un decimal
    private static void validateGrade(BigDecimal grade) {
        if (grade.compareTo(MIN_GRADE) < 0 || grade.compareTo(MAX_GRADE) > 0) {
            throw new IllegalArgumentException("La nota debe estar entre 1.0 y 7.0");
        }
        if (grade.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("La nota admite como máximo un decimal");
        }
    }

    private Section findOwnSection(Long teacherId, Long sectionId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new NotFoundException("Docente no encontrado: " + teacherId);
        }
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Sección no encontrada: " + sectionId));
        if (!section.getTeacherId().equals(teacherId)) {
            throw new IllegalStateException(
                    "El docente " + teacherId + " no es el responsable de la sección: " + sectionId);
        }
        return section;
    }
}
