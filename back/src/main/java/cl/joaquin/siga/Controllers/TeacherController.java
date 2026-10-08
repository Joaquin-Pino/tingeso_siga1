package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherResponseDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherStatusUpdateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherUpdateDTO;
import cl.joaquin.siga.Services.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    public List<TeacherResponseDTO> getAll() {
        return teacherService.getAll();
    }

    @GetMapping("/{id}")
    public TeacherResponseDTO getById(@PathVariable Long id) {
        return teacherService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponseDTO create(@Valid @RequestBody TeacherCreateDTO dto) {
        return teacherService.saveTeacher(dto);
    }

    @PutMapping("/{id}")
    public TeacherResponseDTO update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateDTO dto) {
        return teacherService.updateTeacher(id, dto);
    }

    @PatchMapping("/{id}/status")
    public TeacherResponseDTO updateStatus(@PathVariable Long id, @Valid @RequestBody TeacherStatusUpdateDTO dto) {
        return teacherService.updateStatus(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
    }
}
