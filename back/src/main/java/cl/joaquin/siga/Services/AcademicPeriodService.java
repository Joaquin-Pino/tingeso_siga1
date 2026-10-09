package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodResponseDTO;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final PeriodEnrollmentRepository periodEnrollmentRepository;

    public List<AcademicPeriodResponseDTO> getAll() {
        List<AcademicPeriodResponseDTO> result = new ArrayList<>();
        for (AcademicPeriod period : academicPeriodRepository.findAll()) {
            result.add(toResponseDTO(period));
        }
        return result;
    }

    public AcademicPeriodResponseDTO getById(Long periodId) {
        return toResponseDTO(findOrThrow(periodId));
    }

    public AcademicPeriodResponseDTO createPeriod(AcademicPeriodCreateDTO dto) {
        if (academicPeriodRepository.existsByYearAndSemester(dto.year(), dto.semester())) {
            throw new IllegalArgumentException(
                    "Ya existe el período " + dto.year() + "-" + dto.semester().getNumber());
        }

        AcademicPeriod period = new AcademicPeriod();
        period.setYear(dto.year());
        period.setSemester(dto.semester());
        period.setStatus(PeriodStatus.OPEN); // el período queda abierto al crearse

        return toResponseDTO(academicPeriodRepository.save(period));
    }

    // un período con oferta o matrículas ya está en uso y no se elimina
    public void deletePeriod(Long periodId) {
        AcademicPeriod period = findOrThrow(periodId);

        if (sectionRepository.existsByAcademicPeriodId(periodId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el período tiene secciones asociadas: " + periodId);
        }
        if (periodEnrollmentRepository.existsByAcademicPeriodId(periodId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el período tiene estudiantes matriculados: " + periodId);
        }

        academicPeriodRepository.delete(period);
    }

    // código derivado, ej. 2027-1. Público para que el cierre lo use al armar el snapshot del historial
    public static String periodCode(AcademicPeriod period) {
        return period.getYear() + "-" + period.getSemester().getNumber();
    }

    private AcademicPeriod findOrThrow(Long periodId) {
        return academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Período académico no encontrado: " + periodId));
    }

    private static AcademicPeriodResponseDTO toResponseDTO(AcademicPeriod period) {
        return new AcademicPeriodResponseDTO(
                period.getId(),
                period.getYear(),
                period.getSemester(),
                periodCode(period),
                period.getStatus()
        );
    }
}
