package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanUpdateDTO;
import cl.joaquin.siga.Services.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyPlanResponseDTO create(@Valid @RequestBody StudyPlanCreateDTO dto) {
        return studyPlanService.createStudyPlan(dto);
    }

    @PutMapping("/{id}")
    public StudyPlanResponseDTO update(@PathVariable Long id, @Valid @RequestBody StudyPlanUpdateDTO dto) {
        return studyPlanService.updateStudyPlan(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studyPlanService.deleteStudyPlan(id);
    }
}
