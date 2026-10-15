package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Services.CourseRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseRegistrationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CourseRegistrationService courseRegistrationService;

    private static CourseRegistrationResponseDTO response() {
        return new CourseRegistrationResponseDTO(1L, 3L, 20L, 5L, 10L, LocalDate.of(2027, 3, 1), null, null);
    }

    @Test
    void getAll_withPeriodFilter_returns200() throws Exception {
        when(courseRegistrationService.getByStudent(3L, 10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/students/3/registrations").param("periodId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sectionId").value(20));
    }

    @Test
    void register_valid_returns201() throws Exception {
        when(courseRegistrationService.register(3L, List.of(20L, 21L, 22L))).thenReturn(List.of(response()));

        mockMvc.perform(post("/api/students/3/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionIds\":[20,21,22]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void register_emptyBatch_returns400() throws Exception {
        mockMvc.perform(post("/api/students/3/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ruleViolation_returns409() throws Exception {
        when(courseRegistrationService.register(eq(3L), anyList()))
                .thenThrow(new IllegalStateException("La sección no tiene cupo: 20"));

        mockMvc.perform(post("/api/students/3/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionIds\":[20,21,22]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La sección no tiene cupo: 20"));
    }

    @Test
    void cancel_returns204() throws Exception {
        mockMvc.perform(delete("/api/students/3/registrations/1"))
                .andExpect(status().isNoContent());
        verify(courseRegistrationService).cancel(3L, 1L);
    }

    @Test
    void cancel_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Inscripción 9 no encontrada"))
                .when(courseRegistrationService).cancel(3L, 9L);

        mockMvc.perform(delete("/api/students/3/registrations/9"))
                .andExpect(status().isNotFound());
    }
}
