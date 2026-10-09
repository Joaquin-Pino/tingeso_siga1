package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.SectionDTO.ScheduleBlockDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionCreateDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionResponseDTO;
import cl.joaquin.siga.DTOs.SectionDTO.SectionUpdateDTO;
import cl.joaquin.siga.Entities.University.WeekDay;
import cl.joaquin.siga.Exceptions.NotFoundException;
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

@WebMvcTest(SectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SectionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SectionService sectionService;

    private static final List<ScheduleBlockDTO> BLOCKS =
            List.of(new ScheduleBlockDTO(WeekDay.L, 1), new ScheduleBlockDTO(WeekDay.M, 2));

    private static SectionResponseDTO response() {
        return new SectionResponseDTO(1L, 5L, 10L, 7L, 30, 0, BLOCKS);
    }

    @Test
    void getById_returns200() throws Exception {
        when(sectionService.getById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/sections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleBlocks[1].module").value(2));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(sectionService.getById(9L)).thenThrow(new NotFoundException("Sección no encontrada: 9"));

        mockMvc.perform(get("/api/sections/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(sectionService.createSection(new SectionCreateDTO(5L, 10L, 7L, 30, BLOCKS))).thenReturn(response());

        mockMvc.perform(post("/api/sections").contentType(MediaType.APPLICATION_JSON).content("""
                        {"subjectId": 5, "academicPeriodId": 10, "teacherId": 7, "capacity": 30,
                         "scheduleBlocks": [{"day": "L", "module": 1}, {"day": "M", "module": 2}]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrolledCount").value(0));
    }

    @Test
    void create_capacityOver60_returns400() throws Exception {
        mockMvc.perform(post("/api/sections").contentType(MediaType.APPLICATION_JSON).content("""
                        {"subjectId": 5, "academicPeriodId": 10, "teacherId": 7, "capacity": 61,
                         "scheduleBlocks": [{"day": "L", "module": 1}]}
                        """))
                .andExpect(status().isBadRequest());
        verify(sectionService, never()).createSection(any());
    }

    @Test
    void create_moduleOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/sections").contentType(MediaType.APPLICATION_JSON).content("""
                        {"subjectId": 5, "academicPeriodId": 10, "teacherId": 7, "capacity": 30,
                         "scheduleBlocks": [{"day": "L", "module": 7}]}
                        """))
                .andExpect(status().isBadRequest());
        verify(sectionService, never()).createSection(any());
    }

    @Test
    void create_teacherScheduleConflict_returns409() throws Exception {
        when(sectionService.createSection(any())).thenThrow(new IllegalStateException("tope de horario"));

        mockMvc.perform(post("/api/sections").contentType(MediaType.APPLICATION_JSON).content("""
                        {"subjectId": 5, "academicPeriodId": 10, "teacherId": 7, "capacity": 30,
                         "scheduleBlocks": [{"day": "L", "module": 1}, {"day": "M", "module": 2}]}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(sectionService.updateSection(1L, new SectionUpdateDTO(7L, 40, BLOCKS))).thenReturn(response());

        mockMvc.perform(put("/api/sections/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"teacherId": 7, "capacity": 40,
                         "scheduleBlocks": [{"day": "L", "module": 1}, {"day": "M", "module": 2}]}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingBlocks_returns400() throws Exception {
        mockMvc.perform(put("/api/sections/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\": 7, \"capacity\": 40}"))
                .andExpect(status().isBadRequest());
        verify(sectionService, never()).updateSection(any(), any());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/sections/1"))
                .andExpect(status().isNoContent());
        verify(sectionService).deleteSection(1L);
    }

    @Test
    void delete_withRegistrations_returns409() throws Exception {
        doThrow(new IllegalStateException("tiene inscritos")).when(sectionService).deleteSection(1L);

        mockMvc.perform(delete("/api/sections/1"))
                .andExpect(status().isConflict());
    }
}
