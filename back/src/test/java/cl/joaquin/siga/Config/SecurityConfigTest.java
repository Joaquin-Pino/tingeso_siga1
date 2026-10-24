package cl.joaquin.siga.Config;

import cl.joaquin.siga.Controllers.AcademicPeriodController;
import cl.joaquin.siga.Controllers.CareerController;
import cl.joaquin.siga.Controllers.CourseRegistrationController;
import cl.joaquin.siga.Controllers.GradeController;
import cl.joaquin.siga.Controllers.StudentController;
import cl.joaquin.siga.Controllers.StudyPlanController;
import cl.joaquin.siga.Controllers.SubjectController;
import cl.joaquin.siga.Controllers.TeacherController;
import cl.joaquin.siga.Services.AcademicPeriodService;
import cl.joaquin.siga.Services.CareerService;
import cl.joaquin.siga.Services.CourseRegistrationService;
import cl.joaquin.siga.Services.CurrentUserService;
import cl.joaquin.siga.Services.GradeService;
import cl.joaquin.siga.Services.PeriodCloseService;
import cl.joaquin.siga.Services.SectionService;
import cl.joaquin.siga.Services.StudentService;
import cl.joaquin.siga.Services.StudyPlanService;
import cl.joaquin.siga.Services.SubjectService;
import cl.joaquin.siga.Services.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// matriz de roles por endpoint con los filtros de seguridad activos
@WebMvcTest(controllers = {StudentController.class, TeacherController.class, CourseRegistrationController.class,
        GradeController.class, AcademicPeriodController.class, CareerController.class, StudyPlanController.class,
        SubjectController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean(name = "currentUser") CurrentUserService currentUser;
    @MockitoBean StudentService studentService;
    @MockitoBean TeacherService teacherService;
    @MockitoBean SectionService sectionService;
    @MockitoBean CourseRegistrationService courseRegistrationService;
    @MockitoBean GradeService gradeService;
    @MockitoBean AcademicPeriodService academicPeriodService;
    @MockitoBean PeriodCloseService periodCloseService;
    @MockitoBean CareerService careerService;
    @MockitoBean StudyPlanService studyPlanService;
    @MockitoBean SubjectService subjectService;

    private static RequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static final String REGISTRATION_BODY = "{\"sectionIds\": [1, 2, 3]}";
    private static final String GRADE_BODY = "{\"grade\": 5.5}";

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/careers")).andExpect(status().isUnauthorized());
    }

    // en este slice no está el controller de springdoc: un 404 (y no 401) prueba que el filtro deja pasar la ruta
    @Test
    void apiDocs_arePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void anyRole_canReadCatalog() throws Exception {
        mockMvc.perform(get("/api/careers").with(as("STUDENT"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/academic-periods").with(as("TEACHER"))).andExpect(status().isOk());
    }

    @Test
    void admin_listsStudents() throws Exception {
        mockMvc.perform(get("/api/students").with(as("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void student_listsStudents_returns403() throws Exception {
        mockMvc.perform(get("/api/students").with(as("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void teacher_createsCareer_returns403() throws Exception {
        mockMvc.perform(post("/api/careers").with(as("TEACHER")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_closesPeriod_teacherCannot() throws Exception {
        mockMvc.perform(post("/api/academic-periods/1/close").with(as("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(post("/api/academic-periods/1/close").with(as("TEACHER"))).andExpect(status().isForbidden());
    }

    @Test
    void student_me_onlyForStudents() throws Exception {
        mockMvc.perform(get("/api/students/me").with(as("STUDENT"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/students/me").with(as("TEACHER"))).andExpect(status().isForbidden());
    }

    @Test
    void me_looksUpThePersonByTheJwtSubject() throws Exception {
        mockMvc.perform(get("/api/students/me").with(jwt().jwt(j -> j.subject("kc-student"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/me").with(jwt().jwt(j -> j.subject("kc-teacher"))
                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER")))).andExpect(status().isOk());

        verify(studentService).getByKeycloakId("kc-student");
        verify(teacherService).getByKeycloakId("kc-teacher");
    }

    @Test
    void teacher_me_onlyForTeachers() throws Exception {
        mockMvc.perform(get("/api/teachers/me").with(as("TEACHER"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/me").with(as("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void admin_readsAnyStudent() throws Exception {
        mockMvc.perform(get("/api/students/5").with(as("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void student_readsOwnData_butNotOthers() throws Exception {
        when(currentUser.isStudent(5L)).thenReturn(true);
        when(currentUser.isStudent(6L)).thenReturn(false);

        mockMvc.perform(get("/api/students/5").with(as("STUDENT"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/students/5/academic-history").with(as("STUDENT"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/students/6").with(as("STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students/6/academic-average").with(as("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void student_registersOnlyForSelf() throws Exception {
        when(currentUser.isStudent(5L)).thenReturn(true);
        when(currentUser.isStudent(6L)).thenReturn(false);

        mockMvc.perform(post("/api/students/5/registrations").with(as("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRATION_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/students/6/registrations").with(as("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRATION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canConsultButNotRegister() throws Exception {
        mockMvc.perform(get("/api/students/5/registrations").with(as("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(post("/api/students/5/registrations").with(as("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRATION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cancelsOnlyOwnRegistration() throws Exception {
        when(currentUser.isStudent(5L)).thenReturn(true);
        when(currentUser.isStudent(6L)).thenReturn(false);

        mockMvc.perform(delete("/api/students/5/registrations/1").with(as("STUDENT"))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/students/6/registrations/1").with(as("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void teacher_readsOwnSections_butNotOthers() throws Exception {
        when(currentUser.isTeacher(2L)).thenReturn(true);
        when(currentUser.isTeacher(3L)).thenReturn(false);

        mockMvc.perform(get("/api/teachers/2/sections").with(as("TEACHER"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/3/sections").with(as("TEACHER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teachers/3/sections").with(as("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void teacher_gradesOnlyOwnSections() throws Exception {
        when(currentUser.isTeacher(2L)).thenReturn(true);
        when(currentUser.isTeacher(3L)).thenReturn(false);

        mockMvc.perform(put("/api/teachers/2/sections/9/registrations/1/grade").with(as("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(GRADE_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/teachers/3/sections/9/registrations/1/grade").with(as("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(GRADE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAndStudent_cannotGrade() throws Exception {
        mockMvc.perform(put("/api/teachers/2/sections/9/registrations/1/grade").with(as("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(GRADE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teachers/2/sections/9/registrations").with(as("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
