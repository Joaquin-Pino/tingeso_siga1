package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CourseRegistrationDTO.CourseRegistrationResponseDTO;
import cl.joaquin.siga.DTOs.CourseRegistrationDTO.GradeUpdateDTO;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Services.GradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GradeControllerTest {

    private static final String BASE = "/api/teachers/2/sections/5/registrations";

    @Autowired MockMvc mockMvc;
    @MockitoBean GradeService gradeService;

    private static CourseRegistrationResponseDTO response(BigDecimal grade) {
        return new CourseRegistrationResponseDTO(7L, 3L, 5L, 1L, 10L, LocalDate.of(2027, 3, 1), grade, null);
    }

    @Test
    void getRegistrations_returns200() throws Exception {
        when(gradeService.getSectionRegistrations(2L, 5L)).thenReturn(List.of(response(null)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(3));
    }

    @Test
    void getRegistrations_notTheTeacherOfTheSection_returns409() throws Exception {
        when(gradeService.getSectionRegistrations(2L, 5L)).thenThrow(new IllegalStateException("no es el responsable"));

        mockMvc.perform(get(BASE)).andExpect(status().isConflict());
    }

    @Test
    void registerGrade_valid_returns200() throws Exception {
        when(gradeService.registerGrade(2L, 5L, 7L, new GradeUpdateDTO(new BigDecimal("5.5"))))
                .thenReturn(response(new BigDecimal("5.5")));

        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{\"grade\": 5.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(5.5));
    }

    @Test
    void registerGrade_missingGrade_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(gradeService, never()).registerGrade(any(), any(), any(), any());
    }

    @Test
    void registerGrade_belowOne_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{\"grade\": 0.9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerGrade_aboveSeven_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{\"grade\": 7.1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerGrade_twoDecimals_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{\"grade\": 5.55}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerGrade_closedPeriod_returns409() throws Exception {
        when(gradeService.registerGrade(any(), any(), any(), any())).thenThrow(new IllegalStateException("cerrado"));

        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{\"grade\": 5.5}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerGrade_registrationNotFound_returns404() throws Exception {
        when(gradeService.registerGrade(any(), any(), any(), any())).thenThrow(new NotFoundException("no existe"));

        mockMvc.perform(put(BASE + "/7/grade").contentType(MediaType.APPLICATION_JSON).content("{\"grade\": 5.5}"))
                .andExpect(status().isNotFound());
    }
}
