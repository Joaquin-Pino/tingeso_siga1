package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentResponseDTO;
import cl.joaquin.siga.Services.PeriodEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods/{periodId}/enrollments")
@RequiredArgsConstructor
public class PeriodEnrollmentController {

    private final PeriodEnrollmentService periodEnrollmentService;

    @GetMapping
    public List<PeriodEnrollmentResponseDTO> getAll(@PathVariable Long periodId) {
        return periodEnrollmentService.getByPeriod(periodId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodEnrollmentResponseDTO enroll(@PathVariable Long periodId,
                                              @Valid @RequestBody PeriodEnrollmentCreateDTO dto) {
        return periodEnrollmentService.enroll(periodId, dto);
    }
}
