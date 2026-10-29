package cl.joaquin.siga.Integration;

import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Services.CourseRegistrationService;
import cl.joaquin.siga.Services.DemoDataService;
import cl.joaquin.siga.Services.SeedDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// los datos de demo contra Postgres real: tras el Apéndice A, cada estudiante puede inscribir todo su primer semestre
class DemoDataIntegrationTest extends IntegrationTestBase {

    @Autowired SeedDataService seedDataService;
    @Autowired DemoDataService demoDataService;
    @Autowired CourseRegistrationService courseRegistrationService;

    private void loadCatalog() {
        studyPlanRepository.deleteAll();
        careerRepository.deleteAll();
        assertTrue(seedDataService.seedIfEmpty());
    }

    @Test
    void seedIfEmpty_loadsPeopleAndOffering() {
        loadCatalog();

        assertTrue(demoDataService.seedIfEmpty());

        assertEquals(4, teacherRepository.count());
        assertEquals(4, studentRepository.count());
        assertEquals(1, academicPeriodRepository.count());
        assertEquals(18, sectionRepository.count()); // 6 asignaturas de primer semestre x 3 carreras
        assertEquals(4, periodEnrollmentRepository.count());
    }

    @Test
    void seedIfEmpty_studentsCanRegisterWholeFirstSemester() {
        loadCatalog();
        demoDataService.seedIfEmpty();

        for (Student student : studentRepository.findAll()) {
            List<Long> sectionIds = new ArrayList<>();
            for (Section section : sectionRepository.findAll()) {
                if (subjectRepository.findById(section.getSubjectId()).orElseThrow()
                        .getStudyPlanId().equals(student.getStudyPlanId())) {
                    sectionIds.add(section.getId());
                }
            }
            assertEquals(6, sectionIds.size());
            assertEquals(6, courseRegistrationService.register(student.getId(), sectionIds).size());
        }
    }

    @Test
    void seedIfEmpty_withExistingPeople_doesNothing() {
        loadCatalog();
        demoDataService.seedIfEmpty();

        assertFalse(demoDataService.seedIfEmpty());

        assertEquals(4, teacherRepository.count());
    }
}
