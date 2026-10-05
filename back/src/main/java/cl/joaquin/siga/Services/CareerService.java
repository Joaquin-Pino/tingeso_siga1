package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerResponseDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerStatusUpdateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerUpdateDTO;
import cl.joaquin.siga.Entities.University.Career;
import cl.joaquin.siga.Entities.University.CareerStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentRepository studentRepository;

    public List<CareerResponseDTO> getAll() {
        return careerRepository.findAll().stream()
                .map(CareerService::toResponseDTO)
                .toList();
    }

    public CareerResponseDTO getById(Long careerId) {
        return toResponseDTO(findOrThrow(careerId));
    }

    public CareerResponseDTO createCareer(CareerCreateDTO dto) {
        if (careerRepository.existsByCode(dto.code())) {
            throw new IllegalArgumentException("Ya existe una carrera con el código: " + dto.code());
        }

        Career career = new Career();
        career.setCode(dto.code());
        career.setName(dto.name());
        career.setDescription(dto.description());
        career.setStartYear(dto.startYear());
        career.setVacancies(dto.vacancies());
        career.setStatus(CareerStatus.ACTIVE); // carrera nueva queda activa

        return toResponseDTO(careerRepository.save(career));
    }

    // el código no se modifica una vez creada la carrera
    public CareerResponseDTO updateCareer(Long careerId, CareerUpdateDTO dto) {
        Career career = findOrThrow(careerId);
        career.setName(dto.name());
        career.setDescription(dto.description());
        career.setStartYear(dto.startYear());
        career.setVacancies(dto.vacancies());
        return toResponseDTO(careerRepository.save(career));
    }

    public CareerResponseDTO updateStatus(Long careerId, CareerStatusUpdateDTO dto) {
        Career career = findOrThrow(careerId);
        career.setStatus(dto.status());
        return toResponseDTO(careerRepository.save(career));
    }

    // una carrera con estudiantes o planes no se elimina físicamente; solo cambia su estado
    public void deleteCareer(Long careerId) {
        Career career = findOrThrow(careerId);

        if (studentRepository.existsByCareerId(careerId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: la carrera tiene estudiantes asociados: " + careerId);
        }
        if (studyPlanRepository.existsByCareerId(careerId)) {
            throw new IllegalStateException(
                    "No se puede eliminar: la carrera tiene planes de estudio asociados: " + careerId);
        }

        careerRepository.delete(career);
    }

    private Career findOrThrow(Long careerId) {
        return careerRepository.findById(careerId)
                .orElseThrow(() -> new NotFoundException("Carrera no encontrada: " + careerId));
    }

    private static CareerResponseDTO toResponseDTO(Career career) {
        return new CareerResponseDTO(
                career.getId(),
                career.getCode(),
                career.getName(),
                career.getDescription(),
                career.getStatus(),
                career.getStartYear(),
                career.getVacancies()
        );
    }
}
