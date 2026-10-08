package cl.joaquin.siga.Controllers;

import cl.joaquin.siga.DTOs.AcademicRecordDTO.AcademicRecordResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCareerUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentCreateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentResponseDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStatusUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentStudyPlanUpdateDTO;
import cl.joaquin.siga.DTOs.StudentDTO.StudentUpdateDTO;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Services.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StudentService studentService;

    private static final String VALID_CREATE_BODY = """
            {"keycloakId": "kc", "run": "1-9", "fullName": "Ana", "email": "a@x.cl", "careerId": 1}
            """;

    private static StudentResponseDTO response(StudentStatus status) {
        return new StudentResponseDTO(1L, "1-9", "Ana", "a@x.cl", 1L, 7L, status);
    }

    @Test
    void getAll_returns200() throws Exception {
        when(studentService.getAll()).thenReturn(List.of(response(StudentStatus.REGULAR)));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].run").value("1-9"));
    }

    @Test
    void getById_returns200() throws Exception {
        when(studentService.getById(1L)).thenReturn(response(StudentStatus.REGULAR));

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyPlanId").value(7));
    }

    @Test
    void getAcademicHistory_returns200() throws Exception {
        when(studentService.getAcademicHistory(1L)).thenReturn(List.of(new AcademicRecordResponseDTO(
                5L, 10L, 3L, "TAP101", "Programación", 1, 5, "2026-1", new BigDecimal("5.5"), CourseResult.PASSED)));

        mockMvc.perform(get("/api/students/1/academic-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectCode").value("TAP101"))
                .andExpect(jsonPath("$[0].result").value("PASSED"));
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(studentService.saveStudent(any(StudentCreateDTO.class))).thenReturn(response(StudentStatus.REGULAR));

        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGULAR"));
        verify(studentService).saveStudent(new StudentCreateDTO("kc", "1-9", "Ana", "a@x.cl", 1L));
    }

    @Test
    void create_invalidEmail_returns400WithFieldName() throws Exception {
        String body = """
                {"keycloakId": "kc", "run": "1-9", "fullName": "Ana", "email": "no-es-correo", "careerId": 1}
                """;

        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(startsWith("email:")));
        verify(studentService, never()).saveStudent(any());
    }

    @Test
    void create_missingCareer_returns400() throws Exception {
        String body = """
                {"keycloakId": "kc", "run": "1-9", "fullName": "Ana", "email": "a@x.cl"}
                """;

        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(studentService, never()).saveStudent(any());
    }

    @Test
    void create_dbConstraintViolation_returns409() throws Exception {
        // dos réplicas registran el mismo RUN a la vez: el check del Service pasa, la constraint de BD no
        when(studentService.saveStudent(any())).thenThrow(new DataIntegrityViolationException("uk_student_run"));

        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La operación viola una restricción de integridad de datos"));
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(studentService.updateStudent(1L, new StudentUpdateDTO("Ana María", "b@x.cl")))
                .thenReturn(response(StudentStatus.REGULAR));

        mockMvc.perform(put("/api/students/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Ana María\", \"email\": \"b@x.cl\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_blankName_returns400() throws Exception {
        mockMvc.perform(put("/api/students/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \" \", \"email\": \"b@x.cl\"}"))
                .andExpect(status().isBadRequest());
        verify(studentService, never()).updateStudent(any(), any());
    }

    @Test
    void updateStatus_valid_returns200() throws Exception {
        when(studentService.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.POSTPONED)))
                .thenReturn(response(StudentStatus.POSTPONED));

        mockMvc.perform(patch("/api/students/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"POSTPONED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTPONED"));
    }

    @Test
    void updateStatus_notAssignable_returns400() throws Exception {
        when(studentService.updateStatus(1L, new StudentStatusUpdateDTO(StudentStatus.GRADUATED)))
                .thenThrow(new IllegalArgumentException("no se puede asignar manualmente"));

        mockMvc.perform(patch("/api/students/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"GRADUATED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCareer_valid_returns200() throws Exception {
        when(studentService.updateCareer(1L, new StudentCareerUpdateDTO(2L))).thenReturn(response(StudentStatus.REGULAR));

        mockMvc.perform(patch("/api/students/1/career").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"careerId\": 2}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateCareer_missingCareerId_returns400() throws Exception {
        mockMvc.perform(patch("/api/students/1/career").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(studentService, never()).updateCareer(any(), any());
    }

    @Test
    void updateStudyPlan_valid_returns200() throws Exception {
        when(studentService.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)))
                .thenReturn(response(StudentStatus.REGULAR));

        mockMvc.perform(patch("/api/students/1/study-plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyPlanId\": 6}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStudyPlan_withRegistrations_returns409() throws Exception {
        when(studentService.updateStudyPlan(1L, new StudentStudyPlanUpdateDTO(6L)))
                .thenThrow(new IllegalStateException("tiene inscripciones"));

        mockMvc.perform(patch("/api/students/1/study-plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyPlanId\": 6}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isNoContent());
        verify(studentService).deleteStudent(1L);
    }
}
