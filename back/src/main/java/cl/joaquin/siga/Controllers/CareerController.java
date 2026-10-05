package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerResponseDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerStatusUpdateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.Services.CareerService;
import cl.joaquin.siga.Services.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;
    private final StudyPlanService studyPlanService;

    @GetMapping
    public List<CareerResponseDTO> getAll() {
        return careerService.getAll();
    }

    @GetMapping("/{id}")
    public CareerResponseDTO getById(@PathVariable Long id) {
        return careerService.getById(id);
    }

    @GetMapping("/{id}/study-plans")
    public List<StudyPlanResponseDTO> getStudyPlans(@PathVariable Long id) {
        return studyPlanService.getAllCareerStudyPlan(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CareerResponseDTO create(@Valid @RequestBody CareerCreateDTO dto) {
        return careerService.createCareer(dto);
    }

    @PutMapping("/{id}")
    public CareerResponseDTO update(@PathVariable Long id, @Valid @RequestBody CareerUpdateDTO dto) {
        return careerService.updateCareer(id, dto);
    }

    @PatchMapping("/{id}/status")
    public CareerResponseDTO updateStatus(@PathVariable Long id, @Valid @RequestBody CareerStatusUpdateDTO dto) {
        return careerService.updateStatus(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        careerService.deleteCareer(id);
    }
}
