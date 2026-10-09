package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.Services.AcademicPeriodService;
import cl.joaquin.siga.Services.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;
    private final SectionService sectionService;

    @GetMapping
    public List<AcademicPeriodResponseDTO> getAll() {
        return academicPeriodService.getAll();
    }

    @GetMapping("/{id}")
    public AcademicPeriodResponseDTO getById(@PathVariable Long id) {
        return academicPeriodService.getById(id);
    }

    // la oferta académica del período
    @GetMapping("/{id}/sections")
    public List<SectionResponseDTO> getSections(@PathVariable Long id) {
        return sectionService.getAllPeriodSections(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicPeriodResponseDTO create(@Valid @RequestBody AcademicPeriodCreateDTO dto) {
        return academicPeriodService.createPeriod(dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        academicPeriodService.deletePeriod(id);
    }
}
