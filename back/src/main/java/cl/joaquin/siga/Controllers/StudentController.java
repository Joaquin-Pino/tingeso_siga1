package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.AcademicRecordDTO.AcademicRecordResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStatusUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStudyPlanUpdateDTO;
import cl.joaquin.siga.Services.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// graduate()/eliminate() no se exponen: solo los invoca el cierre de período
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public List<StudentResponseDTO> getAll() {
        return studentService.getAll();
    }

    @GetMapping("/{id}")
    public StudentResponseDTO getById(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @GetMapping("/{id}/academic-history")
    public List<AcademicRecordResponseDTO> getAcademicHistory(@PathVariable Long id) {
        return studentService.getAcademicHistory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponseDTO create(@Valid @RequestBody StudentCreateDTO dto) {
        return studentService.saveStudent(dto);
    }

    @PatchMapping("/{id}/status")
    public StudentResponseDTO updateStatus(@PathVariable Long id, @Valid @RequestBody StudentStatusUpdateDTO dto) {
        return studentService.updateStatus(id, dto);
    }

    @PatchMapping("/{id}/career")
    public StudentResponseDTO updateCareer(@PathVariable Long id, @Valid @RequestBody StudentCareerUpdateDTO dto) {
        return studentService.updateCareer(id, dto);
    }

    @PatchMapping("/{id}/study-plan")
    public StudentResponseDTO updateStudyPlan(@PathVariable Long id, @Valid @RequestBody StudentStudyPlanUpdateDTO dto) {
        return studentService.updateStudyPlan(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }
}
