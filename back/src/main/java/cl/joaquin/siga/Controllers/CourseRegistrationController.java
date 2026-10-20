package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationCreateDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.Services.CourseRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// el estudiante va en la URL y @PreAuthorize verifica que sea el del JWT (el ADMIN solo consulta)
@RestController
@RequestMapping("/api/students/{studentId}/registrations")
@RequiredArgsConstructor
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @currentUser.isStudent(#studentId)")
    public List<CourseRegistrationResponseDTO> getAll(@PathVariable Long studentId,
                                                      @RequestParam(required = false) Long periodId) {
        return courseRegistrationService.getByStudent(studentId, periodId);
    }

    @PostMapping
    @PreAuthorize("@currentUser.isStudent(#studentId)")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CourseRegistrationResponseDTO> register(@PathVariable Long studentId,
                                                        @Valid @RequestBody CourseRegistrationCreateDTO dto) {
        return courseRegistrationService.register(studentId, dto.sectionIds());
    }

    @DeleteMapping("/{registrationId}")
    @PreAuthorize("@currentUser.isStudent(#studentId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long studentId, @PathVariable Long registrationId) {
        courseRegistrationService.cancel(studentId, registrationId);
    }
}
