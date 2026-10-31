package cl.joaquin.siga.Integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Proceso 4: reglas de rango de carreras, períodos y asignaturas. Viven en los Services (los DTOs solo exigen
// presencia), así que un valor fuera de rango debe llegar al Service y volver como 400 sin guardar nada
class CatalogValidationProcessIntegrationTest extends IntegrationTestBase {

    private String subjectBody(int semester, int theory, int exercise, int lab, int credits, String prerequisites) {
        return "{\"studyPlanId\": " + planId + ", \"code\": \"TAP900\", \"name\": \"Asignatura\", \"semester\": " + semester
                + ", \"theoryHours\": " + theory + ", \"exerciseHours\": " + exercise + ", \"labHours\": " + lab
                + ", \"credits\": " + credits + ", \"prerequisiteIds\": " + prerequisites + "}";
    }

    private void postSubject(String body, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/subjects").with(admin()).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
    }

    // ---- asignaturas ----

    @Test
    void subjectSemesterOutOfRange_isRejectedWith400() throws Exception {
        postSubject(subjectBody(0, 2, 0, 0, 5, "[]"), 400);
        postSubject(subjectBody(5, 2, 0, 0, 5, "[]"), 400);

        assertEquals(0, subjectRepository.count());
    }

    @Test
    void subjectCreditsOutOfRange_isRejectedWith400() throws Exception {
        postSubject(subjectBody(1, 2, 0, 0, 0, "[]"), 400);
        postSubject(subjectBody(1, 2, 0, 0, 8, "[]"), 400);

        assertEquals(0, subjectRepository.count());
    }

    @Test
    void subjectNegativeHours_isRejectedWith400() throws Exception {
        postSubject(subjectBody(1, -1, 2, 0, 5, "[]"), 400);

        assertEquals(0, subjectRepository.count());
    }

    @Test
    void subjectWithMoreThanThreePrerequisites_isRejectedWith400() throws Exception {
        Long p1 = subject("TAP101", 1);
        Long p2 = subject("TAP102", 1);
        Long p3 = subject("TAP103", 1);
        Long p4 = subject("TAP104", 1);

        postSubject(subjectBody(2, 2, 0, 0, 5, "[" + p1 + "," + p2 + "," + p3 + "," + p4 + "]"), 400);

        assertEquals(4, subjectRepository.count());
    }

    @Test
    void subjectLimits_areAccepted() throws Exception {
        postSubject(subjectBody(4, 2, 0, 0, 7, "[]"), 201);
    }

    @Test
    void updateSubjectOutOfRange_isRejectedWith400() throws Exception {
        Long id = subject("TAP101", 1);

        mockMvc.perform(put("/api/subjects/{id}", id).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"code\": \"TAP101\", \"name\": \"x\", \"semester\": 5, \"theoryHours\": 2,"
                                + " \"exerciseHours\": 0, \"labHours\": 0, \"credits\": 5, \"prerequisiteIds\": []}"))
                .andExpect(status().isBadRequest());

        assertEquals(1, subjectRepository.findById(id).orElseThrow().getSemester());
    }

    @Test
    void addSubjectToPlanWithStudents_isRejectedWith409() throws Exception {
        student();

        postSubject(subjectBody(1, 2, 0, 0, 5, "[]"), 409);

        assertEquals(0, subjectRepository.count());
    }

    // ---- carreras y períodos ----

    @Test
    void careerWithInvalidYearOrVacancies_isRejectedWith400() throws Exception {
        long before = careerRepository.count();
        for (String body : new String[]{
                "{\"code\": \"INF\", \"name\": \"x\", \"description\": \"x\", \"startYear\": 0, \"vacancies\": 10}",
                "{\"code\": \"INF\", \"name\": \"x\", \"description\": \"x\", \"startYear\": 2020, \"vacancies\": -1}"}) {
            mockMvc.perform(post("/api/careers").with(admin()).contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        assertEquals(before, careerRepository.count());
    }

    @Test
    void careerWithZeroVacancies_isAccepted() throws Exception {
        mockMvc.perform(post("/api/careers").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"code\": \"INF\", \"name\": \"x\", \"description\": \"x\", \"startYear\": 2020, \"vacancies\": 0}"))
                .andExpect(status().isCreated());
    }

    @Test
    void periodWithNonPositiveYear_isRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/academic-periods").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"year\": 0, \"semester\": \"FIRST\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, academicPeriodRepository.count());
    }
}
