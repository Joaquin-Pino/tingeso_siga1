package cl.joaquin.siga.Integration;

import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Services.CourseRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static cl.joaquin.siga.Entities.University.WeekDay.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Proceso 2: el docente registra notas y el administrador cierra el período
// (PUT .../grade y POST /api/academic-periods/{id}/close)
class GradingAndPeriodCloseProcessIntegrationTest extends IntegrationTestBase {

    @Autowired CourseRegistrationService courseRegistrationService;

    private Long periodId;
    private Long teacherId;
    private Long studentId;
    private Long sectionId1, sectionId2, sectionId3;
    private Long registrationId1, registrationId2, registrationId3;

    @BeforeEach
    void registerStudentInThreeSections() {
        periodId = period(2026, Semester.SECOND);
        teacherId = teacher();
        sectionId1 = section(subject("TAP101", 1), periodId, teacherId, 30, block(L, 1));
        sectionId2 = section(subject("TAP102", 1), periodId, teacherId, 30, block(M, 1));
        sectionId3 = section(subject("TAP103", 1), periodId, teacherId, 30, block(W, 1));

        studentId = student();
        enroll(periodId, studentId);
        courseRegistrationService.register(studentId, java.util.List.of(sectionId1, sectionId2, sectionId3));
        registrationId1 = registrationOf(sectionId1).getId();
        registrationId2 = registrationOf(sectionId2).getId();
        registrationId3 = registrationOf(sectionId3).getId();
    }

    private CourseRegistration registrationOf(Long sectionId) {
        return courseRegistrationRepository.findBySectionId(sectionId).get(0);
    }

    private void grade(Long section, Long registration, String grade) throws Exception {
        mockMvc.perform(put("/api/teachers/{t}/sections/{s}/registrations/{r}/grade", teacherId, section, registration)
                        .with(asTeacher(teacherId)).contentType(APPLICATION_JSON)
                        .content("{\"grade\": " + grade + "}"))
                .andExpect(status().isOk());
    }

    // ---- éxitos ----

    @Test
    void teacherRegistersGrade_gradeIsSavedAndResultStaysEmpty() throws Exception {
        mockMvc.perform(put("/api/teachers/{t}/sections/{s}/registrations/{r}/grade", teacherId, sectionId1, registrationId1)
                        .with(asTeacher(teacherId)).contentType(APPLICATION_JSON).content("{\"grade\": 5.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(5.5));

        CourseRegistration saved = registrationOf(sectionId1);
        assertEquals(0, new BigDecimal("5.5").compareTo(saved.getGrade()));
        assertNull(saved.getResult()); // el result lo calcula el cierre
    }

    @Test
    void teacherCorrectsGradeWhilePeriodIsOpen_lastValueWins() throws Exception {
        grade(sectionId1, registrationId1, "5.5");
        grade(sectionId1, registrationId1, "3.0");

        assertEquals(0, new BigDecimal("3.0").compareTo(registrationOf(sectionId1).getGrade()));
    }

    @Test
    void closePeriodWithAllGrades_setsResultsCreatesHistoryAndClosesPeriod() throws Exception {
        grade(sectionId1, registrationId1, "5.5");
        grade(sectionId2, registrationId2, "3.0");
        grade(sectionId3, registrationId3, "4.0");

        mockMvc.perform(post("/api/academic-periods/{id}/close", periodId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsCreated").value(3))
                .andExpect(jsonPath("$.period.status").value("CLOSED"));

        assertEquals(CourseResult.PASSED, registrationOf(sectionId1).getResult());
        assertEquals(CourseResult.FAILED, registrationOf(sectionId2).getResult());
        assertEquals(CourseResult.PASSED, registrationOf(sectionId3).getResult()); // 4,0 aprueba
        assertEquals(3, academicRecordRepository.findByStudentId(studentId).size());
        assertEquals(PeriodStatus.CLOSED, academicPeriodRepository.findById(periodId).orElseThrow().getStatus());
    }

    // ---- rechazos ----

    @Test
    void gradeOutOfRange_isRejectedWith400() throws Exception {
        mockMvc.perform(put("/api/teachers/{t}/sections/{s}/registrations/{r}/grade", teacherId, sectionId1, registrationId1)
                        .with(asTeacher(teacherId)).contentType(APPLICATION_JSON).content("{\"grade\": 7.5}"))
                .andExpect(status().isBadRequest());

        assertNull(registrationOf(sectionId1).getGrade());
    }

    @Test
    void gradeBelowOneOrWithTwoDecimals_isRejectedWith400() throws Exception {
        for (String invalid : new String[]{"0.9", "5.55"}) {
            mockMvc.perform(put("/api/teachers/{t}/sections/{s}/registrations/{r}/grade", teacherId, sectionId1, registrationId1)
                            .with(asTeacher(teacherId)).contentType(APPLICATION_JSON)
                            .content("{\"grade\": " + invalid + "}"))
                    .andExpect(status().isBadRequest());
        }

        assertNull(registrationOf(sectionId1).getGrade());
    }

    @Test
    void gradeLimits_areAccepted() throws Exception {
        grade(sectionId1, registrationId1, "1.0");
        grade(sectionId1, registrationId1, "7.0");
    }

    // ---- historial docente ----

    private void gradeAllAndClose() throws Exception {
        grade(sectionId1, registrationId1, "5.5");
        grade(sectionId2, registrationId2, "3.0");
        grade(sectionId3, registrationId3, "4.0");
        mockMvc.perform(post("/api/academic-periods/{id}/close", periodId).with(admin())).andExpect(status().isOk());
    }

    @Test
    void teachingHistory_isEmptyWhilePeriodIsOpen() throws Exception {
        mockMvc.perform(get("/api/teachers/{id}/teaching-history", teacherId).with(asTeacher(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void closePeriod_recordsEverySectionOfTheTeacher() throws Exception {
        gradeAllAndClose();

        mockMvc.perform(get("/api/teachers/{id}/teaching-history", teacherId).with(asTeacher(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].periodCode").value("2026-2"))
                .andExpect(jsonPath("$[0].enrolledCount").value(1));
        assertEquals(3, teachingRecordRepository.findByTeacherId(teacherId).size());
    }

    @Test
    void teachingHistory_survivesTeacherBeingDeactivated() throws Exception {
        gradeAllAndClose();

        mockMvc.perform(patch("/api/teachers/{id}/status", teacherId).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"status\": \"INACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teachers/{id}/teaching-history", teacherId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void teachingHistory_otherTeacherIsForbidden_adminIsAllowed() throws Exception {
        gradeAllAndClose();
        Long otherTeacher = teacher();

        mockMvc.perform(get("/api/teachers/{id}/teaching-history", teacherId).with(asTeacher(otherTeacher)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teachers/{id}/teaching-history", teacherId).with(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/{id}/teaching-history", teacherId).with(asStudent(studentId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherGradesSectionOfAnotherTeacher_isRejected() throws Exception {
        Long otherTeacher = teacher();

        mockMvc.perform(put("/api/teachers/{t}/sections/{s}/registrations/{r}/grade", otherTeacher, sectionId1, registrationId1)
                        .with(asTeacher(otherTeacher)).contentType(APPLICATION_JSON).content("{\"grade\": 6.0}"))
                .andExpect(status().isConflict());

        assertNull(registrationOf(sectionId1).getGrade());
    }

    @Test
    void closePeriodWithUngradedRegistrations_isRejectedAndPeriodStaysOpen() throws Exception {
        grade(sectionId1, registrationId1, "5.5");

        mockMvc.perform(post("/api/academic-periods/{id}/close", periodId).with(admin()))
                .andExpect(status().isConflict());

        assertEquals(PeriodStatus.OPEN, academicPeriodRepository.findById(periodId).orElseThrow().getStatus());
        assertEquals(0, academicRecordRepository.findByStudentId(studentId).size());
        assertNull(registrationOf(sectionId1).getResult());
    }

    @Test
    void gradeAfterPeriodIsClosed_isRejected() throws Exception {
        grade(sectionId1, registrationId1, "5.5");
        grade(sectionId2, registrationId2, "5.0");
        grade(sectionId3, registrationId3, "5.0");
        mockMvc.perform(post("/api/academic-periods/{id}/close", periodId).with(admin()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teachers/{t}/sections/{s}/registrations/{r}/grade", teacherId, sectionId1, registrationId1)
                        .with(asTeacher(teacherId)).contentType(APPLICATION_JSON).content("{\"grade\": 2.0}"))
                .andExpect(status().isConflict());

        assertEquals(0, new BigDecimal("5.5").compareTo(registrationOf(sectionId1).getGrade()));
    }
}
