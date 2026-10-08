package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanUpdateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectResponseDTO;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Services.StudyPlanService;
import cl.joaquin.siga.Services.SubjectService;
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

@WebMvcTest(StudyPlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StudyPlanService studyPlanService;
    @MockitoBean SubjectService subjectService;

    private static StudyPlanResponseDTO response(String code) {
        return new StudyPlanResponseDTO(5L, 1L, code, StudyPlanStatus.CURRENT);
    }

    @Test
    void getSubjects_delegatesToSubjectService() throws Exception {
        when(subjectService.getAllStudyPlanSubjects(5L)).thenReturn(List.of(
                new SubjectResponseDTO(20L, 5L, "TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, List.of(10L))));

        mockMvc.perform(get("/api/study-plans/5/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TAP302"))
                .andExpect(jsonPath("$[0].prerequisiteIds[0]").value(10));
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(studyPlanService.createStudyPlan(new StudyPlanCreateDTO(1L, "2026.1"))).thenReturn(response("2026.1"));

        mockMvc.perform(post("/api/study-plans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"careerId\": 1, \"code\": \"2026.1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CURRENT"));
    }

    @Test
    void create_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/study-plans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"careerId\": 1, \"code\": \"\"}"))
                .andExpect(status().isBadRequest());
        verify(studyPlanService, never()).createStudyPlan(any());
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(studyPlanService.updateStudyPlan(5L, new StudyPlanUpdateDTO("2026.2"))).thenReturn(response("2026.2"));

        mockMvc.perform(put("/api/study-plans/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"2026.2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2026.2"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/study-plans/5"))
                .andExpect(status().isNoContent());
        verify(studyPlanService).deleteStudyPlan(5L);
    }

    @Test
    void delete_currentPlan_returns409() throws Exception {
        doThrow(new IllegalStateException("plan vigente")).when(studyPlanService).deleteStudyPlan(5L);

        mockMvc.perform(delete("/api/study-plans/5"))
                .andExpect(status().isConflict());
    }
}
