package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentResponseDTO;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodEnrollment;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// la matrícula administrativa real está fuera de alcance: el ADMIN solo registra que el estudiante está matriculado
@Service
@RequiredArgsConstructor
public class PeriodEnrollmentService { // matricula

    private final PeriodEnrollmentRepository periodEnrollmentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final StudentRepository studentRepository;
    private final Clock clock;

    public List<PeriodEnrollmentResponseDTO> getByPeriod(Long periodId) {
        if (!academicPeriodRepository.existsById(periodId)) {
            throw new NotFoundException("Período académico no encontrado: " + periodId);
        }
        List<PeriodEnrollmentResponseDTO> result = new ArrayList<>();
        for (PeriodEnrollment enrollment : periodEnrollmentRepository.findByAcademicPeriodId(periodId)) {
            result.add(toResponseDTO(enrollment));
        }
        return result;
    }

    public PeriodEnrollmentResponseDTO enroll(Long periodId, PeriodEnrollmentCreateDTO dto) {
        AcademicPeriod period = academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Período académico no encontrado: " + periodId));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("El período académico está cerrado: " + periodId);
        }
        if (!studentRepository.existsById(dto.studentId())) {
            throw new NotFoundException("Estudiante no encontrado: " + dto.studentId());
        }
        if (periodEnrollmentRepository.existsByStudentIdAndAcademicPeriodId(dto.studentId(), periodId)) {
            throw new IllegalArgumentException(
                    "El estudiante " + dto.studentId() + " ya está matriculado en el período: " + periodId);
        }

        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setStudentId(dto.studentId());
        enrollment.setAcademicPeriodId(periodId);
        enrollment.setEnrollmentDate(LocalDate.now(clock));
        return toResponseDTO(periodEnrollmentRepository.save(enrollment));
    }

    private static PeriodEnrollmentResponseDTO toResponseDTO(PeriodEnrollment enrollment) {
        return new PeriodEnrollmentResponseDTO(
                enrollment.getId(),
                enrollment.getStudentId(),
                enrollment.getAcademicPeriodId(),
                enrollment.getEnrollmentDate()
        );
    }
}
