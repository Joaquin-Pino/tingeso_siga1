package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherResponseDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherStatusUpdateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherUpdateDTO;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Entities.People.TeacherStatus;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    public List<TeacherResponseDTO> getAll() {
        List<TeacherResponseDTO> result = new ArrayList<>();
        for (Teacher teacher : teacherRepository.findAll()) {
            result.add(toResponseDTO(teacher));
        }
        return result;
    }

    public TeacherResponseDTO getById(Long teacherId) {
        return toResponseDTO(findOrThrow(teacherId));
    }

    public TeacherResponseDTO saveTeacher(TeacherCreateDTO dto) {
        if (teacherRepository.existsByRun(dto.run())) {
            throw new IllegalArgumentException("El RUN ya está registrado: " + dto.run());
        }
        if (teacherRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya está registrado: " + dto.email());
        }

        Teacher teacher = new Teacher();
        teacher.setKeycloakId(dto.keycloakId());
        teacher.setRun(dto.run());
        teacher.setFullName(dto.fullName());
        teacher.setEmail(dto.email());
        teacher.setProfessionalTitle(dto.professionalTitle());
        teacher.setAcademicDegree(dto.academicDegree());
        teacher.setStatus(TeacherStatus.ACTIVE); // docente queda activo al registrarse

        return toResponseDTO(teacherRepository.save(teacher));
    }

    // el RUN no se modifica una vez registrado el docente
    public TeacherResponseDTO updateTeacher(Long teacherId, TeacherUpdateDTO dto) {
        Teacher teacher = findOrThrow(teacherId);

        // el correo debe seguir siendo único en el sistema
        if (!dto.email().equals(teacher.getEmail()) && teacherRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya está registrado: " + dto.email());
        }

        teacher.setFullName(dto.fullName());
        teacher.setEmail(dto.email());
        teacher.setProfessionalTitle(dto.professionalTitle());
        teacher.setAcademicDegree(dto.academicDegree());
        return toResponseDTO(teacherRepository.save(teacher));
    }

    // el cambio de estado solo toca el campo status: sus secciones (su historia) no se modifican
    public TeacherResponseDTO updateStatus(Long teacherId, TeacherStatusUpdateDTO dto) {
        Teacher teacher = findOrThrow(teacherId);


        if (dto.status() == TeacherStatus.INACTIVE) {
            List<Section> sections = sectionRepository.findByTeacherId(teacherId);
            for (Section section : sections) {
                // no puede pasar a INACTIVE si tiene secciones en un período OPEN
                if (academicPeriodRepository.existsByIdAndStatus(section.getAcademicPeriodId(), PeriodStatus.OPEN)) {
                    throw new IllegalStateException(
                            "No se puede desactivar: el docente tiene secciones en un período abierto: " + teacherId);
                }
            }
        }

        teacher.setStatus(dto.status());
        return toResponseDTO(teacherRepository.save(teacher));
    }

    // un docente con secciones no se elimina físicamente; solo cambia su estado
    public void deleteTeacher(Long teacherId) {
        Teacher teacher = findOrThrow(teacherId);

        if (sectionRepository.existsByTeacherId(teacherId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el docente tiene secciones asociadas: " + teacherId);
        }

        teacherRepository.delete(teacher);
    }

    private Teacher findOrThrow(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Docente no encontrado: " + teacherId));
    }

    private static TeacherResponseDTO toResponseDTO(Teacher teacher) {
        return new TeacherResponseDTO(
                teacher.getId(),
                teacher.getRun(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getProfessionalTitle(),
                teacher.getAcademicDegree(),
                teacher.getStatus()
        );
    }
}
