package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationCreateDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.Services.CourseRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// el estudiante va en la URL hasta que llegue Keycloak; entonces saldrá del JWT
@RestController
@RequestMapping("/api/students/{studentId}/registrations")
@RequiredArgsConstructor
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    @GetMapping
    public List<CourseRegistrationResponseDTO> getAll(@PathVariable Long studentId,
                                                      @RequestParam(required = false) Long periodId) {
        return courseRegistrationService.getByStudent(studentId, periodId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<CourseRegistrationResponseDTO> register(@PathVariable Long studentId,
                                                        @Valid @RequestBody CourseRegistrationCreateDTO dto) {
        return courseRegistrationService.register(studentId, dto.sectionIds());
    }

    @DeleteMapping("/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long studentId, @PathVariable Long registrationId) {
        courseRegistrationService.cancel(studentId, registrationId);
    }
}
