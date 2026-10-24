package cl.joaquin.siga.Services;

import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.DTOs.AcademicRecordDTO.AcademicAverageResponseDTO;
import cl.joaquin.siga.DTOs.AcademicRecordDTO.AcademicRecordResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStatusUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStudyPlanUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentUpdateDTO;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final KeycloakAdminService keycloakAdminService;
    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final AcademicRecordRepository academicRecordRepository;

    public List<StudentResponseDTO> getAll() {
        List<StudentResponseDTO> result = new ArrayList<>();
        for (Student student : studentRepository.findAll()) {
            result.add(toResponseDTO(student));
        }
        return result;
    }

    public StudentResponseDTO getById(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));
        return toResponseDTO(student);
    }

    // el estudiante autenticado: el sub del JWT de Keycloak es su keycloakId
    public StudentResponseDTO getByKeycloakId(String keycloakId) {
        Student student = studentRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new NotFoundException("No hay un estudiante asociado a la cuenta autenticada"));
        return toResponseDTO(student);
    }

    public List<AcademicRecordResponseDTO> getAcademicHistory(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Estudiante no encontrado: " + studentId);
        }
        List<AcademicRecordResponseDTO> result = new ArrayList<>();
        for (AcademicRecord record : academicRecordRepository.findByStudentId(studentId)) {
            result.add(toAcademicRecordResponseDTO(record));
        }
        return result;
    }

    // promedio ponderado por SCT (aprobadas y reprobadas), calculado al consultar.
    // periodId opcional: con él es el promedio del semestre; sin él, el de todo el historial
    public AcademicAverageResponseDTO getAcademicAverage(Long studentId, Long periodId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Estudiante no encontrado: " + studentId);
        }
        List<AcademicRecord> records = periodId == null
                ? academicRecordRepository.findByStudentId(studentId)
                : academicRecordRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId);

        BigDecimal weightedSum = BigDecimal.ZERO;
        int totalCredits = 0;
        for (AcademicRecord record : records) {
            weightedSum = weightedSum.add(record.getFinalGrade().multiply(BigDecimal.valueOf(record.getCredits())));
            totalCredits += record.getCredits();
        }
        BigDecimal average = totalCredits == 0
                ? null
                : weightedSum.divide(BigDecimal.valueOf(totalCredits), 1, RoundingMode.HALF_UP);
        return new AcademicAverageResponseDTO(studentId, average, records.size(), totalCredits);
    }

    public StudentResponseDTO saveStudent(StudentCreateDTO dto) {
        if (studentRepository.existsByRun(dto.run())) {
            throw new IllegalArgumentException("El RUN ya está registrado: " + dto.run());
        }
        if (studentRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya está registrado: " + dto.email());
        }

        // estudiante queda asociado al plan de estudios vigente, tambien se revisa que carrera este vigente
        StudyPlan currentPlan = resolveCurrentStudyPlanOfActiveCareer(dto.careerId());

        Student student = new Student();
        student.setRun(dto.run());
        student.setFullName(dto.fullName());
        student.setEmail(dto.email());
        student.setCareerId(dto.careerId());
        student.setStudyPlanId(currentPlan.getId());
        student.setStatus(StudentStatus.REGULAR); // estudiante queda guardado como regular

        // la cuenta de Keycloak se crea al final de las validaciones; su id (el sub del JWT) es el keycloakId
        String keycloakId = keycloakAdminService.createUser(dto.email(), dto.fullName(), dto.temporaryPassword(), "STUDENT");
        student.setKeycloakId(keycloakId);
        try {
            return toResponseDTO(studentRepository.save(student));
        } catch (RuntimeException e) {
            // si la BD rechaza al estudiante (ej. carrera concurrente) no se deja la cuenta huérfana
            keycloakAdminService.deleteUser(keycloakId);
            throw e;
        }
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
            throw new NotFoundException("Carrera no encontrada: " + careerId);
        }
        if (!careerRepository.existsByIdAndStatus(careerId, CareerStatus.ACTIVE)) {
            throw new IllegalArgumentException("La carrera no está activa: " + careerId);
        }

        return studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT)
                .orElseThrow(() -> new IllegalStateException(
                        "La carrera no tiene un plan de estudios vigente: " + careerId));
    }

    // el RUN no se modifica una vez registrado el estudiante
    public StudentResponseDTO updateStudent(Long studentId, StudentUpdateDTO dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));

        // el correo debe seguir siendo único en el sistema
        if (!dto.email().equals(student.getEmail()) && studentRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya está registrado: " + dto.email());
        }

        student.setFullName(dto.fullName());
        student.setEmail(dto.email());
        return toResponseDTO(studentRepository.save(student));
    }

    public StudentResponseDTO updateStatus(Long studentId, StudentStatusUpdateDTO dto) {
        // el administrador solo puede asignar REGULAR, POSTPONED o TEMPORARY_WITHDRAWAL;
        // GRADUATED/ELIMINATED los asigna únicamente el cierre de período
        if (dto.status() != StudentStatus.REGULAR
                && dto.status() != StudentStatus.POSTPONED
                && dto.status() != StudentStatus.TEMPORARY_WITHDRAWAL) {
            throw new IllegalArgumentException(
                    "El estado " + dto.status() + " no se puede asignar manualmente; solo REGULAR, POSTPONED o TEMPORARY_WITHDRAWAL");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));

        // un estudiante egresado o eliminado por el cierre no se puede modificar manualmente
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.ELIMINATED) {
            throw new IllegalStateException(
                    "No se puede modificar el estado: el estudiante está " + student.getStatus() + ": " + studentId);
        }

        student.setStatus(dto.status());
        return toResponseDTO(studentRepository.save(student));
    }

    public StudentResponseDTO updateCareer(Long studentId, StudentCareerUpdateDTO dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));

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

    public StudentResponseDTO updateStudyPlan(Long studentId, StudentStudyPlanUpdateDTO dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));

        if (courseRegistrationRepository.existsByStudentId(studentId)) {
            throw new IllegalStateException(
                    "No se puede cambiar plan de estudios: el estudiante tiene inscripciones: " + studentId);
        }
        if (academicRecordRepository.existsByStudentId(studentId)) {
            throw new IllegalStateException(
                    "No se puede cambiar plan de estudios: el estudiante tiene historial académico: " + studentId);
        }

        StudyPlan newPlan = studyPlanRepository.findById(dto.studyPlanId())
                .orElseThrow(() -> new NotFoundException("Plan de estudios no encontrado: " + dto.studyPlanId()));

        // el plan debe pertenecer a la carrera del estudiante (careerId/studyPlanId deben ser coherentes)
        if (!newPlan.getCareerId().equals(student.getCareerId())) {
            throw new IllegalArgumentException(
                    "El plan de estudios no pertenece a la carrera del estudiante: " + dto.studyPlanId());
        }

        student.setStudyPlanId(newPlan.getId());

        return toResponseDTO(studentRepository.save(student));
    }

    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));

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
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));
        student.setStatus(StudentStatus.GRADUATED);
        studentRepository.save(student);
    }

    public void eliminate(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));
        student.setStatus(StudentStatus.ELIMINATED);
        studentRepository.save(student);
    }
}
