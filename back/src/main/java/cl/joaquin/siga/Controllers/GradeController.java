package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.GradeUpdateDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.SectionRegistrationResponseDTO;
import cl.joaquin.siga.Services.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// el docente va en la URL y @PreAuthorize verifica que sea el del JWT
@RestController
@RequestMapping("/api/teachers/{teacherId}/sections/{sectionId}/registrations")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping
    @PreAuthorize("@currentUser.isTeacher(#teacherId)")
    public List<SectionRegistrationResponseDTO> getRegistrations(@PathVariable Long teacherId,
                                                                @PathVariable Long sectionId) {
        return gradeService.getSectionRegistrations(teacherId, sectionId);
    }

    @PutMapping("/{registrationId}/grade")
    @PreAuthorize("@currentUser.isTeacher(#teacherId)")
    public CourseRegistrationResponseDTO registerGrade(@PathVariable Long teacherId,
                                                       @PathVariable Long sectionId,
                                                       @PathVariable Long registrationId,
                                                       @Valid @RequestBody GradeUpdateDTO dto) {
        return gradeService.registerGrade(teacherId, sectionId, registrationId, dto);
    }
}
