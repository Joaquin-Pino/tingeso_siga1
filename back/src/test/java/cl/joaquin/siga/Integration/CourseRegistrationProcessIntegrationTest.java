package cl.joaquin.siga.Integration;

import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cl.joaquin.siga.Entities.University.WeekDay.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Proceso 1: inscripción de asignaturas del estudiante (POST/DELETE /api/students/{id}/registrations)
class CourseRegistrationProcessIntegrationTest extends IntegrationTestBase {

    private Long periodId;
    private Long studentId;
    // semestre 1: s1..s3 sin choques, s5 choca con s1; s4 es el cuarto ramo libre
    private Long s1, s2, s3, s4, s5;
    // semestre 2, con s1 como prerrequisito
    private Long sem2;

    @BeforeEach
    void offerSubjectsAndMatriculateStudent() {
        periodId = period(2026, Semester.SECOND);
        Long teacherA = teacher();
        Long teacherB = teacher();

        Long subject1 = subject("TAP101", 1);
        Long subject2 = subject("TAP102", 1);
        Long subject3 = subject("TAP103", 1);
        Long subject4 = subject("TAP104", 1);
        Long subject5 = subject("TAP105", 1);
        Long subjectSem2 = subject("TAP201", 2, subject1);

        s1 = section(subject1, periodId, teacherA, 30, block(L, 1));
        s2 = section(subject2, periodId, teacherA, 30, block(M, 1));
        s3 = section(subject3, periodId, teacherA, 30, block(W, 1));
        s4 = section(subject4, periodId, teacherB, 30, block(J, 1));
        s5 = section(subject5, periodId, teacherB, 30, block(L, 1));
        sem2 = section(subjectSem2, periodId, teacherB, 30, block(V, 1));

        studentId = student();
        enroll(periodId, studentId);
    }

    private int enrolledCount(Long sectionId) {
        return sectionRepository.findById(sectionId).orElseThrow().getEnrolledCount();
    }

    private List<CourseRegistration> registrations() {
        return courseRegistrationRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId);
    }

    // ---- éxitos ----

    @Test
    void registerThreeSections_createsRegistrationsAndTakesSeats() throws Exception {
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2, s3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].studentId").value(studentId));

        assertEquals(3, registrations().size());
        assertEquals(1, enrolledCount(s1));
        assertEquals(1, enrolledCount(s2));
        assertEquals(1, enrolledCount(s3));
    }

    @Test
    void addFourthSectionInSecondRequest_countsAlreadyRegistered() throws Exception {
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2, s3)))
                .andExpect(status().isCreated());

        // un lote de una sola sección es válido porque junto con lo ya inscrito suma 4
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));

        assertEquals(4, registrations().size());
        assertEquals(1, enrolledCount(s4));
    }

    @Test
    void cancelRegistration_deletesRowAndReleasesSeat() throws Exception {
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2, s3, s4)))
                .andExpect(status().isCreated());
        CourseRegistration toCancel = null;
        for (CourseRegistration r : registrations()) {
            if (r.getSectionId().equals(s4)) toCancel = r;
        }

        mockMvc.perform(delete("/api/students/{id}/registrations/{rid}", studentId, toCancel.getId())
                        .with(asStudent(studentId)))
                .andExpect(status().isNoContent());

        assertEquals(3, registrations().size());
        assertEquals(0, enrolledCount(s4));
    }

    // ---- rechazos ----

    @Test
    void registerWithoutPassingPrerequisite_isRejectedAndNothingIsSaved() throws Exception {
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s2, s3, sem2)))
                .andExpect(status().isConflict());

        assertEquals(0, registrations().size());
        assertEquals(0, enrolledCount(s2));
    }

    @Test
    void registerSectionsWithScheduleClash_isRejected() throws Exception {
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2, s5)))
                .andExpect(status().isConflict());

        assertEquals(0, registrations().size());
    }

    @Test
    void registerWithFullSection_rollsBackTheSeatsAlreadyTaken() throws Exception {
        Section full = sectionRepository.findById(s3).orElseThrow();
        full.setEnrolledCount(full.getCapacity());
        sectionRepository.save(full);

        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2, s3)))
                .andExpect(status().isConflict());

        assertEquals(0, registrations().size());
        assertEquals(0, enrolledCount(s1));
        assertEquals(0, enrolledCount(s2));
    }

    @Test
    void registerFewerThanThreeSubjects_isRejected() throws Exception {
        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(studentId)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2)))
                .andExpect(status().isConflict());

        assertEquals(0, registrations().size());
    }

    @Test
    void registerForAnotherStudent_isForbidden() throws Exception {
        Long other = student();

        mockMvc.perform(post("/api/students/{id}/registrations", studentId)
                        .with(asStudent(other)).contentType(APPLICATION_JSON).content(sectionIds(s1, s2, s3)))
                .andExpect(status().isForbidden());

        assertEquals(0, registrations().size());
    }
}
