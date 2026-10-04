package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicRecordDTO.AcademicRecordResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStatusUpdateDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.CareerStatus;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final AcademicRecordRepository academicRecordRepository;

    public List<StudentResponseDTO> getAll() {
        return studentRepository.findAll().stream()
                .map(StudentService::toResponseDTO)
                .toList();
    }

    public StudentResponseDTO getById(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + studentId));
        return toResponseDTO(student);
    }

    public List<AcademicRecordResponseDTO> getAcademicHistory(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Estudiante no encontrado: " + studentId);
        }
        return academicRecordRepository.findByStudentId(studentId).stream()
                .map(StudentService::toAcademicRecordResponseDTO)
                .toList();
    }

    public StudentResponseDTO saveStudent(StudentCreateDTO dto) {
        if (studentRepository.existsByRun(dto.run())) {
            throw new IllegalArgumentException("El RUN ya está registrado: " + dto.run());
        }
        if (studentRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya está registrado: " + dto.email());
        }

        StudyPlan currentPlan = resolveCurrentStudyPlanOfActiveCareer(dto.careerId());

        Student student = new Student();
        student.setKeycloakId(dto.keycloakId());
        student.setRun(dto.run());
        student.setFullName(dto.fullName());
        student.setEmail(dto.email());
        student.setCareerId(dto.careerId());
        student.setStudyPlanId(currentPlan.getId());
        student.setStatus(StudentStatus.REGULAR); // estudiante queda guardado como regular

        Student saved = studentRepository.save(student);
        return toResponseDTO(saved);
    }

    private static StudentResponseDTO toResponseDTO(Student student) {
        return new StudentResponseDTO(
                student.getId(),
                student.getRun(),
                student.getFullName(),
                student.getEmail(),
                student.getCareerId(),
                student.getStudyPlanId(),
                student.getStatus()
        );
    }

    private static AcademicRecordResponseDTO toAcademicRecordResponseDTO(AcademicRecord record) {
        return new AcademicRecordResponseDTO(
                record.getId(),
                record.getSubjectId(),
                record.getAcademicPeriodId(),
                record.getSubjectCode(),
                record.getSubjectName(),
                record.getSubjectSemester(),
                record.getCredits(),
                record.getPeriodCode(),
                record.getFinalGrade(),
                record.getResult()
        );
    }

    private StudyPlan resolveCurrentStudyPlanOfActiveCareer(Long careerId) {
        if (!careerRepository.existsById(careerId)) {
            throw new IllegalArgumentException("Carrera no encontrada: " + careerId);
        }
        if (!careerRepository.existsByIdAndStatus(careerId, CareerStatus.ACTIVE)) {
            throw new IllegalArgumentException("La carrera no está activa: " + careerId);
        }

        return studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT)
                .orElseThrow(() -> new IllegalStateException(
                        "La carrera no tiene un plan de estudios vigente: " + careerId));
    }

    public StudentResponseDTO updateStatus(Long studentId, StudentStatusUpdateDTO dto) {
        if (dto.status() == StudentStatus.GRADUATED || dto.status() == StudentStatus.ELIMINATED) {
            throw new IllegalArgumentException(
                    "El estado " + dto.status() + " solo lo asigna el cierre de período, no se puede asignar manualmente");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + studentId));

        student.setStatus(dto.status());
        return toResponseDTO(studentRepository.save(student));
    }

    public StudentResponseDTO updateCareer(Long studentId, StudentCareerUpdateDTO dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + studentId));

        if (courseRegistrationRepository.existsByStudentId(studentId)) {
            throw new IllegalStateException(
                    "No se puede cambiar la carrera: el estudiante tiene inscripciones: " + studentId);
        }
        if (academicRecordRepository.existsByStudentId(studentId)) {
            throw new IllegalStateException(
                    "No se puede cambiar la carrera: el estudiante tiene historial académico: " + studentId);
        }

        // Solo se recalcula el plan si la carrera efectivamente cambia. Si se deja la misma carrera,
        // el plan del estudiante no se toca aunque la carrera ya tenga un plan vigente distinto:
        // crear un plan nuevo no debe migrar a los estudiantes que ya estaban
        if (!dto.careerId().equals(student.getCareerId())) {
            StudyPlan currentPlan = resolveCurrentStudyPlanOfActiveCareer(dto.careerId());
            student.setCareerId(dto.careerId());
            student.setStudyPlanId(currentPlan.getId());
        }

        return toResponseDTO(studentRepository.save(student));
    }

    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + studentId));

        if (courseRegistrationRepository.existsByStudentId(studentId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el estudiante tiene inscripciones: " + studentId);
        }
        if (academicRecordRepository.existsByStudentId(studentId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el estudiante tiene historial académico: " + studentId);
        }

        studentRepository.delete(student);
    }

    // Solo debe invocarlos el proceso de cierre de período (épica 7); no exponer en un endpoint
    // de edición directa, ya que GRADUATED/ELIMINATED no se asignan manualmente.
    public void graduate(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + studentId));
        student.setStatus(StudentStatus.GRADUATED);
        studentRepository.save(student);
    }

    public void eliminate(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + studentId));
        student.setStatus(StudentStatus.ELIMINATED);
        studentRepository.save(student);
    }

}
