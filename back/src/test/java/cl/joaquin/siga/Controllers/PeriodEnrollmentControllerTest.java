package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentResponseDTO;
import cl.joaquin.siga.Services.PeriodEnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PeriodEnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PeriodEnrollmentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PeriodEnrollmentService periodEnrollmentService;

    private static PeriodEnrollmentResponseDTO response() {
        return new PeriodEnrollmentResponseDTO(1L, 3L, 10L, LocalDate.of(2027, 3, 1));
    }

    @Test
    void getAll_returns200() throws Exception {
        when(periodEnrollmentService.getByPeriod(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/academic-periods/10/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(3));
    }

    @Test
    void enroll_valid_returns201() throws Exception {
        when(periodEnrollmentService.enroll(10L, new PeriodEnrollmentCreateDTO(3L))).thenReturn(response());

        mockMvc.perform(post("/api/academic-periods/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.academicPeriodId").value(10));
    }

    @Test
    void enroll_missingStudent_returns400() throws Exception {
        mockMvc.perform(post("/api/academic-periods/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
