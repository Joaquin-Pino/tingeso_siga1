package cl.joaquin.siga.Integration;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.PeriodCloseResponseDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Entities.People.TeacherStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Services.CourseRegistrationService;
import cl.joaquin.siga.Services.DemoDataService;
import cl.joaquin.siga.Services.PeriodCloseService;
import cl.joaquin.siga.Services.SeedDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// los datos de demo contra Postgres real: cuatro períodos (tres cerrados con historial y uno abierto) y estudiantes
// en las situaciones que se muestran en la demo
class DemoDataIntegrationTest extends IntegrationTestBase {

    @Autowired SeedDataService seedDataService;
    @Autowired DemoDataService demoDataService;
    @Autowired CourseRegistrationService courseRegistrationService;
    @Autowired PeriodCloseService periodCloseService;

    private void loadDemo() {
        studyPlanRepository.deleteAll();
        careerRepository.deleteAll();
        assertTrue(seedDataService.seedIfEmpty());
        assertTrue(demoDataService.seedIfEmpty());
    }

    private Student student(int number) {
        for (Student student : studentRepository.findAll()) {
            if (student.getEmail().equals("estudiante" + number + "@ipt.cl")) {
                return student;
            }
        }
        throw new IllegalStateException("No existe el estudiante de demo " + number);
    }

    private AcademicPeriod openPeriod() {
        List<AcademicPeriod> open = new ArrayList<>();
        for (AcademicPeriod period : academicPeriodRepository.findAll()) {
            if (period.getStatus() == PeriodStatus.OPEN) {
                open.add(period);
            }
        }
        assertEquals(1, open.size());
        return open.get(0);
    }

    @Test
    void seedIfEmpty_loadsPeopleAndFourPeriods() {
        loadDemo();

        assertEquals(6, teacherRepository.count());
        assertEquals(7, studentRepository.count());
        assertEquals(4, academicPeriodRepository.count());
        assertEquals(1, openPeriod().getYear() == 2026 ? 1 : 0);

        int inactive = 0;
        for (Teacher teacher : teacherRepository.findAll()) {
            if (teacher.getStatus() == TeacherStatus.INACTIVE) {
                inactive++;
                assertFalse(sectionRepository.existsByTeacherId(teacher.getId()));
            }
        }
        assertEquals(1, inactive);
        assertFalse(teachingRecordRepository.findAll().isEmpty()); // los períodos cerrados dejaron historial docente
    }

    @Test
    void seedIfEmpty_buildsAcademicHistory() {
        loadDemo();

        // estudiante 1: 3 semestres completos aprobados; estudiante 3: reprobó TAD202 y la aprobó después
        assertEquals(18, academicRecordRepository.findByStudentIdAndResult(
                student(1).getId(), CourseResult.PASSED).size());
        int failed = 0;
        int passedRetake = 0;
        for (AcademicRecord record : academicRecordRepository.findByStudentId(student(3).getId())) {
            if (record.getSubjectCode().equals("TAD202") && record.getResult() == CourseResult.FAILED) {
                failed++;
            }
            if (record.getSubjectCode().equals("TAD202") && record.getResult() == CourseResult.PASSED) {
                passedRetake++;
            }
        }
        assertEquals(1, failed);
        assertEquals(1, passedRetake);
        assertEquals(StudentStatus.REGULAR, student(3).getStatus());
    }

    @Test
    void closingTheOpenPeriod_graduatesFailsAndEliminates() {
        loadDemo();
        Long periodId = openPeriod().getId();

        PeriodCloseResponseDTO result = periodCloseService.closePeriod(periodId);

        assertEquals(List.of(student(1).getId()), result.graduatedStudentIds());
        assertEquals(List.of(student(5).getId()), result.eliminatedStudentIds());
        assertEquals(StudentStatus.GRADUATED, student(1).getStatus());
        assertEquals(StudentStatus.ELIMINATED, student(5).getStatus());

        // estudiante 2: sin inscripciones en 2026-2, no tiene registros y sigue regular
        assertEquals(StudentStatus.REGULAR, student(2).getStatus());
        assertTrue(academicRecordRepository.findByStudentIdAndAcademicPeriodId(student(2).getId(), periodId).isEmpty());
    }

    @Test
    void seedIfEmpty_student2HasPendingTap306ThatClashesWithTap404() {
        loadDemo();
        Student student = student(2);
        Long periodId = openPeriod().getId();

        // aprobó todo hasta 3º menos TAP306, que reprobó en 2026-1
        assertEquals(17, academicRecordRepository.findByStudentIdAndResult(student.getId(), CourseResult.PASSED).size());
        assertTrue(courseRegistrationRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId).isEmpty());

        Map<String, Long> sectionByCode = new HashMap<>();
        for (Section section : sectionRepository.findByAcademicPeriodId(periodId)) {
            sectionByCode.put(subjectRepository.findById(section.getSubjectId()).orElseThrow().getCode(), section.getId());
        }

        // TAP306 + TAP404 chocan; sin TAP404 el lote es válido (4º semestre exige TAP306 inscrita)
        assertThrows(IllegalStateException.class, () -> courseRegistrationService.register(student.getId(),
                List.of(sectionByCode.get("TAP306"), sectionByCode.get("TAP404"), sectionByCode.get("TAP401"))));
        assertThrows(IllegalStateException.class, () -> courseRegistrationService.register(student.getId(),
                List.of(sectionByCode.get("TAP401"), sectionByCode.get("TAP402"), sectionByCode.get("TAP403"))));
        assertEquals(4, courseRegistrationService.register(student.getId(), List.of(sectionByCode.get("TAP306"),
                sectionByCode.get("TAP401"), sectionByCode.get("TAP402"), sectionByCode.get("TAP403"))).size());
    }

    @Test
    void seedIfEmpty_studentWithoutRegistrationsCanRegisterWholeFourthSemester() {
        loadDemo();
        Student student = student(4);
        Long periodId = openPeriod().getId();

        List<Long> sectionIds = new ArrayList<>();
        for (Section section : sectionRepository.findByAcademicPeriodId(periodId)) {
            if (subjectRepository.findById(section.getSubjectId()).orElseThrow().getCode().startsWith("TCS4")) {
                sectionIds.add(section.getId());
            }
        }
        assertEquals(6, sectionIds.size());
        assertEquals(6, courseRegistrationService.register(student.getId(), sectionIds).size());
    }

    @Test
    void seedIfEmpty_registeredStudentCanCancelDownToThreeSubjects() {
        loadDemo();
        Student student = student(4);
        Long periodId = openPeriod().getId();
        List<Long> sectionIds = new ArrayList<>();
        for (Section section : sectionRepository.findByAcademicPeriodId(periodId)) {
            if (subjectRepository.findById(section.getSubjectId()).orElseThrow().getCode().startsWith("TCS4")) {
                sectionIds.add(section.getId());
            }
        }
        courseRegistrationService.register(student.getId(), sectionIds);

        List<CourseRegistration> registrations =
                courseRegistrationRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId);
        for (int i = 0; i < 3; i++) {
            courseRegistrationService.cancel(student.getId(), registrations.get(i).getId());
        }

        assertThrows(IllegalStateException.class,
                () -> courseRegistrationService.cancel(student.getId(), registrations.get(3).getId()));
    }

    @Test
    void seedIfEmpty_withExistingPeople_doesNothing() {
        loadDemo();

        assertFalse(demoDataService.seedIfEmpty());

        assertEquals(6, teacherRepository.count());
    }
}
