package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerResponseDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerStatusUpdateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.Entities.University.CareerStatus;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Services.CareerService;
import cl.joaquin.siga.Services.StudyPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// sin filtros: la seguridad (Keycloak) aún no está configurada; aquí se prueba solo la capa web
@WebMvcTest(CareerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CareerControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CareerService careerService;
    @MockitoBean StudyPlanService studyPlanService;

    private static final String VALID_BODY = """
            {"code": "INF", "name": "Informática", "description": "desc", "startYear": 2020, "vacancies": 50}
            """;

    private static CareerResponseDTO response(CareerStatus status) {
        return new CareerResponseDTO(1L, "INF", "Informática", "desc", status, 2020, 50);
    }

    @Test
    void getAll_returns200WithList() throws Exception {
        when(careerService.getAll()).thenReturn(List.of(response(CareerStatus.ACTIVE)));

        mockMvc.perform(get("/api/careers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("INF"));
    }

    @Test
    void getById_returns200() throws Exception {
        when(careerService.getById(1L)).thenReturn(response(CareerStatus.ACTIVE));

        mockMvc.perform(get("/api/careers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getById_notFound_returns404WithMessage() throws Exception {
        when(careerService.getById(9L)).thenThrow(new NotFoundException("Carrera no encontrada: 9"));

        mockMvc.perform(get("/api/careers/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Carrera no encontrada: 9"));
    }

    @Test
    void getStudyPlans_delegatesToStudyPlanService() throws Exception {
        when(studyPlanService.getAllCareerStudyPlan(1L))
                .thenReturn(List.of(new StudyPlanResponseDTO(5L, 1L, "2026.1", StudyPlanStatus.CURRENT)));

        mockMvc.perform(get("/api/careers/1/study-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("2026.1"));
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(careerService.createCareer(any(CareerCreateDTO.class))).thenReturn(response(CareerStatus.ACTIVE));

        mockMvc.perform(post("/api/careers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        verify(careerService).createCareer(new CareerCreateDTO("INF", "Informática", "desc", 2020, 50));
    }

    @Test
    void create_invalidBody_returns400AndSkipsService() throws Exception {
        String body = """
                {"code": "", "name": "Informática", "description": "desc", "startYear": 2020, "vacancies": -1}
                """;

        mockMvc.perform(post("/api/careers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(careerService, never()).createCareer(any());
    }

    @Test
    void create_duplicateCode_returns400() throws Exception {
        when(careerService.createCareer(any())).thenThrow(new IllegalArgumentException("Ya existe"));

        mockMvc.perform(post("/api/careers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe"));
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(careerService.updateCareer(eq(1L), any(CareerUpdateDTO.class))).thenReturn(response(CareerStatus.ACTIVE));

        mockMvc.perform(put("/api/careers/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Informática", "description": "desc", "startYear": 2020, "vacancies": 50}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_valid_returns200() throws Exception {
        when(careerService.updateStatus(1L, new CareerStatusUpdateDTO(CareerStatus.INACTIVE)))
                .thenReturn(response(CareerStatus.INACTIVE));

        mockMvc.perform(patch("/api/careers/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/careers/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(careerService, never()).updateStatus(any(), any());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/careers/1"))
                .andExpect(status().isNoContent());
        verify(careerService).deleteCareer(1L);
    }

    @Test
    void delete_withDependencies_returns409() throws Exception {
        doThrow(new IllegalStateException("tiene estudiantes")).when(careerService).deleteCareer(1L);

        mockMvc.perform(delete("/api/careers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("tiene estudiantes"));
    }
}
