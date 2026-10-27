package cl.joaquin.siga.Integration;

import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Services.SeedDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

// la carga inicial del Apéndice A contra Postgres real: 3 carreras, 72 asignaturas y sus prerrequisitos
class SeedDataIntegrationTest extends IntegrationTestBase {

    @Autowired SeedDataService seedDataService;

    private void emptyCatalog() {
        subjectPrerequisiteRepository.deleteAll();
        subjectRepository.deleteAll();
        studyPlanRepository.deleteAll();
        careerRepository.deleteAll();
    }

    @Test
    void seedIfEmpty_loadsAppendixA() {
        emptyCatalog();

        assertTrue(seedDataService.seedIfEmpty());

        assertEquals(3, careerRepository.count());
        assertEquals(72, subjectRepository.count());
        assertEquals(3, studyPlanRepository.count());
        for (StudyPlan plan : studyPlanRepository.findAll()) {
            assertEquals(StudyPlanStatus.CURRENT, plan.getStatus());
            assertEquals(24, subjectRepository.findByStudyPlanId(plan.getId()).size());
        }
        // TAP405 exige TAP301, TAP302 y TAP306
        Long tap405 = null;
        for (var subject : subjectRepository.findAll()) {
            if (subject.getCode().equals("TAP405")) tap405 = subject.getId();
        }
        assertEquals(3, subjectService.getById(tap405).prerequisiteIds().size());
    }

    @Test
    void seedIfEmpty_withExistingData_doesNothing() {
        // @BeforeEach de la base ya dejó una carrera
        assertFalse(seedDataService.seedIfEmpty());

        assertEquals(1, careerRepository.count());
        assertEquals(0, subjectRepository.count());
    }
}
