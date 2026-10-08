package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.TeacherDTO.TeacherCreateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherResponseDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherStatusUpdateDTO;
import cl.joaquin.siga.DTOs.TeacherDTO.TeacherUpdateDTO;
import cl.joaquin.siga.Entities.People.AcademicDegree;
import cl.joaquin.siga.Entities.People.TeacherStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Services.TeacherService;
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

@WebMvcTest(TeacherController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TeacherService teacherService;

    private static TeacherResponseDTO response(TeacherStatus status) {
        return new TeacherResponseDTO(1L, "1-9", "Pedro", "p@x.cl", "Ingeniero", AcademicDegree.MASTER, status);
    }

    @Test
    void getAll_returns200() throws Exception {
        when(teacherService.getAll()).thenReturn(List.of(response(TeacherStatus.ACTIVE)));

        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].academicDegree").value("MASTER"));
    }

    @Test
    void getById_returns200() throws Exception {
        when(teacherService.getById(1L)).thenReturn(response(TeacherStatus.ACTIVE));

        mockMvc.perform(get("/api/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run").value("1-9"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(teacherService.getById(9L)).thenThrow(new NotFoundException("Docente no encontrado: 9"));

        mockMvc.perform(get("/api/teachers/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(teacherService.saveTeacher(any(TeacherCreateDTO.class))).thenReturn(response(TeacherStatus.ACTIVE));

        mockMvc.perform(post("/api/teachers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"keycloakId": "kc", "run": "1-9", "fullName": "Pedro", "email": "p@x.cl",
                         "professionalTitle": "Ingeniero", "academicDegree": "MASTER"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        verify(teacherService).saveTeacher(
                new TeacherCreateDTO("kc", "1-9", "Pedro", "p@x.cl", "Ingeniero", AcademicDegree.MASTER));
    }

    @Test
    void create_missingDegree_returns400() throws Exception {
        mockMvc.perform(post("/api/teachers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"keycloakId": "kc", "run": "1-9", "fullName": "Pedro", "email": "p@x.cl",
                         "professionalTitle": "Ingeniero"}
                        """))
                .andExpect(status().isBadRequest());
        verify(teacherService, never()).saveTeacher(any());
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(teacherService.updateTeacher(1L,
                new TeacherUpdateDTO("Pedro P", "q@x.cl", "Magíster", AcademicDegree.DOCTORATE)))
                .thenReturn(response(TeacherStatus.ACTIVE));

        mockMvc.perform(put("/api/teachers/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName": "Pedro P", "email": "q@x.cl", "professionalTitle": "Magíster",
                         "academicDegree": "DOCTORATE"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void update_invalidEmail_returns400() throws Exception {
        mockMvc.perform(put("/api/teachers/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName": "Pedro", "email": "x", "professionalTitle": "Ingeniero",
                         "academicDegree": "MASTER"}
                        """))
                .andExpect(status().isBadRequest());
        verify(teacherService, never()).updateTeacher(any(), any());
    }

    @Test
    void updateStatus_valid_returns200() throws Exception {
        when(teacherService.updateStatus(1L, new TeacherStatusUpdateDTO(TeacherStatus.INACTIVE)))
                .thenReturn(response(TeacherStatus.INACTIVE));

        mockMvc.perform(patch("/api/teachers/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateStatus_sectionsInOpenPeriod_returns409() throws Exception {
        when(teacherService.updateStatus(1L, new TeacherStatusUpdateDTO(TeacherStatus.INACTIVE)))
                .thenThrow(new IllegalStateException("secciones en período abierto"));

        mockMvc.perform(patch("/api/teachers/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INACTIVE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/teachers/1"))
                .andExpect(status().isNoContent());
        verify(teacherService).deleteTeacher(1L);
    }
}
