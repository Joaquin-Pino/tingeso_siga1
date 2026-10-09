package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Services.AcademicPeriodService;
import cl.joaquin.siga.Services.SectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcademicPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
class AcademicPeriodControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AcademicPeriodService academicPeriodService;
    @MockitoBean SectionService sectionService;

    private static AcademicPeriodResponseDTO response() {
        return new AcademicPeriodResponseDTO(1L, 2027, Semester.FIRST, "2027-1", PeriodStatus.OPEN);
    }

    @Test
    void getAll_returns200() throws Exception {
        when(academicPeriodService.getAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/academic-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("2027-1"));
    }

    @Test
    void getById_returns200() throws Exception {
        when(academicPeriodService.getById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/academic-periods/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(academicPeriodService.getById(9L)).thenThrow(new NotFoundException("Período no encontrado: 9"));

        mockMvc.perform(get("/api/academic-periods/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSections_returns200() throws Exception {
        when(sectionService.getAllPeriodSections(1L)).thenReturn(List.of(new SectionResponseDTO(
                3L, 5L, 1L, 7L, 30, 0, List.of(new ScheduleBlockDTO(WeekDay.L, 1)))));

        mockMvc.perform(get("/api/academic-periods/1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleBlocks[0].day").value("L"));
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(academicPeriodService.createPeriod(new AcademicPeriodCreateDTO(2027, Semester.FIRST))).thenReturn(response());

        mockMvc.perform(post("/api/academic-periods").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\": 2027, \"semester\": \"FIRST\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("2027-1"));
    }

    @Test
    void create_missingSemester_returns400() throws Exception {
        mockMvc.perform(post("/api/academic-periods").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\": 2027}"))
                .andExpect(status().isBadRequest());
        verify(academicPeriodService, never()).createPeriod(any());
    }

    @Test
    void create_duplicate_returns400() throws Exception {
        when(academicPeriodService.createPeriod(any())).thenThrow(new IllegalArgumentException("Ya existe"));

        mockMvc.perform(post("/api/academic-periods").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\": 2027, \"semester\": \"FIRST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/academic-periods/1"))
                .andExpect(status().isNoContent());
        verify(academicPeriodService).deletePeriod(1L);
    }

    @Test
    void delete_withSections_returns409() throws Exception {
        doThrow(new IllegalStateException("tiene secciones")).when(academicPeriodService).deletePeriod(1L);

        mockMvc.perform(delete("/api/academic-periods/1"))
                .andExpect(status().isConflict());
    }
}
