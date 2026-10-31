package cl.joaquin.siga.Integration;

import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Proceso 3: el administrador arma la oferta académica (POST /api/sections)
class SectionCreationProcessIntegrationTest extends IntegrationTestBase {

    private Long periodId;
    private Long teacherId;
    private Long oneBlockSubject;   // 2 horas semanales → 1 bloque
    private Long twoBlockSubject;   // 4 horas semanales → 2 bloques

    @BeforeEach
    void setUpOffering() {
        periodId = period(2026, Semester.SECOND);
        teacherId = teacher();
        oneBlockSubject = subject("TAP101", 1);
        twoBlockSubject = subjectService.createSubject(new cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO(
                planId, "TAP102", "Asignatura TAP102", 1, 2, 2, 0, 5, java.util.Set.of())).id();
    }

    private String body(Long subjectId, Long period, Long teacher, int capacity, String blocks) {
        return "{\"subjectId\": " + subjectId + ", \"academicPeriodId\": " + period + ", \"teacherId\": " + teacher
                + ", \"capacity\": " + capacity + ", \"scheduleBlocks\": [" + blocks + "]}";
    }

    private static String blockJson(String day, int module) {
        return "{\"day\": \"" + day + "\", \"module\": " + module + "}";
    }

    // ---- éxitos ----

    @Test
    void createSection_startsEmptyAndSavesItsBlocks() throws Exception {
        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(oneBlockSubject, periodId, teacherId, 30, blockJson("L", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrolledCount").value(0))
                .andExpect(jsonPath("$.scheduleBlocks.length()").value(1));

        assertEquals(1, sectionRepository.findByAcademicPeriodId(periodId).size());
        assertEquals(1, sectionScheduleBlockRepository.count());
    }

    @Test
    void createSectionOfFourHourSubject_savesTwoBlocks() throws Exception {
        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(twoBlockSubject, periodId, teacherId, 25,
                                blockJson("M", 2) + "," + blockJson("J", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleBlocks.length()").value(2));

        assertEquals(2, sectionScheduleBlockRepository.count());
    }

    @Test
    void sameTeacherSameSlotInAnotherPeriod_isAllowed() throws Exception {
        // el Service ya no deja un segundo período abierto, así que el siguiente se guarda directo
        var next = new cl.joaquin.siga.Entities.University.AcademicPeriod();
        next.setYear(2027);
        next.setSemester(Semester.FIRST);
        next.setStatus(cl.joaquin.siga.Entities.University.PeriodStatus.OPEN);
        Long nextPeriod = academicPeriodRepository.save(next).getId();
        section(oneBlockSubject, periodId, teacherId, 30, block(cl.joaquin.siga.Entities.University.WeekDay.L, 1));

        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(oneBlockSubject, nextPeriod, teacherId, 30, blockJson("L", 1))))
                .andExpect(status().isCreated());

        assertEquals(2, sectionRepository.count());
    }

    // ---- rechazos ----

    @Test
    void teacherAlreadyTeachingInThatSlot_isRejected() throws Exception {
        section(oneBlockSubject, periodId, teacherId, 30, block(cl.joaquin.siga.Entities.University.WeekDay.L, 1));

        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(twoBlockSubject, periodId, teacherId, 30,
                                blockJson("L", 1) + "," + blockJson("W", 1))))
                .andExpect(status().isConflict());

        assertEquals(1, sectionRepository.count());
    }

    @Test
    void wrongNumberOfBlocksForWeeklyHours_isRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(twoBlockSubject, periodId, teacherId, 30, blockJson("L", 1))))
                .andExpect(status().isBadRequest());

        assertEquals(0, sectionRepository.count());
        assertEquals(0, sectionScheduleBlockRepository.count());
    }

    @Test
    void capacityOutOfRange_isRejectedWith400() throws Exception {
        for (int capacity : new int[]{61, 0}) {
            mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                            .content(body(oneBlockSubject, periodId, teacherId, capacity, blockJson("L", 1))))
                    .andExpect(status().isBadRequest());
        }

        assertEquals(0, sectionRepository.count());
    }

    @Test
    void capacity60_isAccepted() throws Exception {
        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(oneBlockSubject, periodId, teacherId, 60, blockJson("L", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capacity").value(60));
    }

    @Test
    void moduleOutOfRange_isRejectedWith400() throws Exception {
        for (int module : new int[]{0, 7}) {
            mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                            .content(body(oneBlockSubject, periodId, teacherId, 30, blockJson("L", module))))
                    .andExpect(status().isBadRequest());
        }

        assertEquals(0, sectionRepository.count());
    }

    @Test
    void closedPeriod_isRejected() throws Exception {
        AcademicPeriod period = academicPeriodRepository.findById(periodId).orElseThrow();
        period.setStatus(PeriodStatus.CLOSED);
        academicPeriodRepository.save(period);

        mockMvc.perform(post("/api/sections").with(admin()).contentType(APPLICATION_JSON)
                        .content(body(oneBlockSubject, periodId, teacherId, 30, blockJson("L", 1))))
                .andExpect(status().isConflict());

        assertEquals(0, sectionRepository.count());
    }

    @Test
    void nonAdminUser_isForbidden() throws Exception {
        mockMvc.perform(post("/api/sections").with(asTeacher(teacherId)).contentType(APPLICATION_JSON)
                        .content(body(oneBlockSubject, periodId, teacherId, 30, blockJson("L", 1))))
                .andExpect(status().isForbidden());

        assertEquals(0, sectionRepository.count());
    }
}
