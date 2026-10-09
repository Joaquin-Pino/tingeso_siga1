package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionUpdateDTO;
import cl.joaquin.siga.Services.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/{id}")
    public SectionResponseDTO getById(@PathVariable Long id) {
        return sectionService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponseDTO create(@Valid @RequestBody SectionCreateDTO dto) {
        return sectionService.createSection(dto);
    }

    @PutMapping("/{id}")
    public SectionResponseDTO update(@PathVariable Long id, @Valid @RequestBody SectionUpdateDTO dto) {
        return sectionService.updateSection(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sectionService.deleteSection(id);
    }
}
