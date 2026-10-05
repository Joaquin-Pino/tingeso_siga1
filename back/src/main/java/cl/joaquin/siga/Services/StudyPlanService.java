package cl.joaquin.siga.Services;

import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanUpdateDTO;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerRepository careerRepository;
    private final StudentRepository studentRepository;

    public List<StudyPlanResponseDTO> getAllCareerStudyPlan(Long careerId) {
        if (!careerRepository.existsById(careerId)) {
            throw new NotFoundException("Carrera no encontrada: " + careerId);
        }
        return studyPlanRepository.findByCareerId(careerId).stream()
                .map(StudyPlanService::toResponseDTO)
                .toList();
    }

    // El plan nuevo queda vigente y el vigente anterior de la carrera pasa a NOT_CURRENT. Va en una
    // transacción para que no queden dos vigentes ni ninguno si algo falla a la mitad
    @Transactional
    public StudyPlanResponseDTO createStudyPlan(StudyPlanCreateDTO dto) {
        if (!careerRepository.existsById(dto.careerId())) {
            throw new NotFoundException("Carrera no encontrada: " + dto.careerId());
        }
        if (studyPlanRepository.existsByCareerIdAndCode(dto.careerId(), dto.code())) {
            throw new IllegalArgumentException(
                    "Ya existe un plan con el código " + dto.code() + " en la carrera: " + dto.careerId());
        }

        // se desactiva y flushea el vigente anterior antes de insertar el nuevo, para no violar
        // el índice único parcial de "un solo plan vigente por carrera"
        studyPlanRepository.findByCareerIdAndStatus(dto.careerId(), StudyPlanStatus.CURRENT)
                .ifPresent(previous -> {
                    previous.setStatus(StudyPlanStatus.NOT_CURRENT);
                    studyPlanRepository.saveAndFlush(previous);
                });

        StudyPlan plan = new StudyPlan();
        plan.setCareerId(dto.careerId());
        plan.setCode(dto.code());
        plan.setStatus(StudyPlanStatus.CURRENT);

        return toResponseDTO(studyPlanRepository.save(plan));
    }

    public StudyPlanResponseDTO updateStudyPlan(Long studyPlanId, StudyPlanUpdateDTO dto) {
        StudyPlan plan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new NotFoundException("Plan de estudios no encontrado: " + studyPlanId));

        // el código debe seguir siendo único dentro de la carrera
        if (!dto.code().equals(plan.getCode())
                && studyPlanRepository.existsByCareerIdAndCode(plan.getCareerId(), dto.code())) {
            throw new IllegalArgumentException(
                    "Ya existe un plan con el código " + dto.code() + " en la carrera: " + plan.getCareerId());
        }

        plan.setCode(dto.code());
        return toResponseDTO(studyPlanRepository.save(plan));
    }

    public void deleteStudyPlan(Long studyPlanId) {
        StudyPlan plan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new NotFoundException("Plan de estudios no encontrado: " + studyPlanId));

        // borrar el plan vigente dejaría a la carrera sin plan y sin poder registrar estudiantes
        if (plan.getStatus() == StudyPlanStatus.CURRENT) {
            throw new IllegalStateException(
                    "No se puede eliminar el plan de estudios vigente de la carrera: " + studyPlanId);
        }
        if (studentRepository.existsByStudyPlanId(studyPlanId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el plan de estudios tiene estudiantes asociados: " + studyPlanId);
        }

        studyPlanRepository.delete(plan);
    }

    private static StudyPlanResponseDTO toResponseDTO(StudyPlan plan) {
        return new StudyPlanResponseDTO(
                plan.getId(),
                plan.getCareerId(),
                plan.getCode(),
                plan.getStatus()
        );
    }
}
