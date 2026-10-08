package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectUpdateDTO;
import cl.joaquin.siga.Services.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubjectControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SubjectService subjectService;

    private static final SubjectResponseDTO RESPONSE =
            new SubjectResponseDTO(20L, 1L, "TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, List.of(10L));

    @Test
    void getById_returns200() throws Exception {
        when(subjectService.getById(20L)).thenReturn(RESPONSE);

        mockMvc.perform(get("/api/subjects/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credits").value(5));
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(subjectService.createSubject(any(SubjectCreateDTO.class))).thenReturn(RESPONSE);

        mockMvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studyPlanId": 1, "code": "TAP302", "name": "Ingeniería de Software", "semester": 3,
                         "theoryHours": 2, "exerciseHours": 2, "labHours": 0, "credits": 5, "prerequisiteIds": [10]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20));
        verify(subjectService).createSubject(
                new SubjectCreateDTO(1L, "TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, Set.of(10L)));
    }

    @Test
    void create_semesterOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studyPlanId": 1, "code": "TAP302", "name": "Ingeniería de Software", "semester": 5,
                         "theoryHours": 2, "exerciseHours": 2, "labHours": 0, "credits": 5}
                        """))
                .andExpect(status().isBadRequest());
        verify(subjectService, never()).createSubject(any());
    }

    @Test
    void create_moreThanThreePrerequisites_returns400() throws Exception {
        mockMvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studyPlanId": 1, "code": "TAP401", "name": "Taller", "semester": 4,
                         "theoryHours": 2, "exerciseHours": 2, "labHours": 0, "credits": 5,
                         "prerequisiteIds": [1, 2, 3, 4]}
                        """))
                .andExpect(status().isBadRequest());
        verify(subjectService, never()).createSubject(any());
    }

    @Test
    void create_creditsOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studyPlanId": 1, "code": "TAP302", "name": "Ingeniería de Software", "semester": 3,
                         "theoryHours": 2, "exerciseHours": 2, "labHours": 0, "credits": 8}
                        """))
                .andExpect(status().isBadRequest());
        verify(subjectService, never()).createSubject(any());
    }

    @Test
    void create_invalidHoursSum_returns400FromService() throws Exception {
        when(subjectService.createSubject(any())).thenThrow(new IllegalArgumentException("suma de horas"));

        mockMvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studyPlanId": 1, "code": "TAP302", "name": "Ingeniería de Software", "semester": 3,
                         "theoryHours": 4, "exerciseHours": 2, "labHours": 2, "credits": 5}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("suma de horas"));
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(subjectService.updateSubject(20L,
                new SubjectUpdateDTO("TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, Set.of())))
                .thenReturn(RESPONSE);

        mockMvc.perform(put("/api/subjects/20").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "TAP302", "name": "Ingeniería de Software", "semester": 3,
                         "theoryHours": 2, "exerciseHours": 2, "labHours": 0, "credits": 5, "prerequisiteIds": []}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void update_negativeHours_returns400() throws Exception {
        mockMvc.perform(put("/api/subjects/20").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "TAP302", "name": "Ingeniería de Software", "semester": 3,
                         "theoryHours": -1, "exerciseHours": 2, "labHours": 0, "credits": 5}
                        """))
                .andExpect(status().isBadRequest());
        verify(subjectService, never()).updateSubject(any(), any());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/subjects/20"))
                .andExpect(status().isNoContent());
        verify(subjectService).deleteSubject(20L);
    }

    @Test
    void delete_isPrerequisite_returns409() throws Exception {
        doThrow(new IllegalStateException("es prerrequisito")).when(subjectService).deleteSubject(20L);

        mockMvc.perform(delete("/api/subjects/20"))
                .andExpect(status().isConflict());
    }
}
