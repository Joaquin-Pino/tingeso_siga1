package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodResponseDTO;
import cl.joaquin.siga.DTOs.AcademicPeriodDTO.PeriodCloseResponseDTO;
import cl.joaquin.siga.Entities.People.AcademicRecord;
import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.StudentStatus;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.CourseRegistration;
import cl.joaquin.siga.Entities.University.CourseResult;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Section;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// proceso de cierre de período. Los pasos se pueden reintentar sin duplicar efectos
// (los AcademicRecord solo se crean si no existen), para poder convertirlo en una saga más adelante
@Service
@RequiredArgsConstructor
public class PeriodCloseService {
    private static final BigDecimal PASSING_GRADE = new BigDecimal("4.0");
    private static final int FAILS_TO_BE_ELIMINATED = 2;

    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public PeriodCloseResponseDTO closePeriod(Long periodId) {
        AcademicPeriod period = academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Período académico no encontrado: " + periodId));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("El período académico ya está cerrado: " + periodId);
        }

        // 1. todas las inscripciones deben tener nota
        List<CourseRegistration> registrations = courseRegistrationRepository.findByAcademicPeriodId(periodId);
        validateAllGraded(registrations);

        // 2. result de cada inscripción y snapshot en el historial
        int recordsCreated = createRecords(period, registrations);

        // 3. estudiantes que se gradúan o quedan eliminados
        List<Long> graduated = new ArrayList<>();
        List<Long> eliminated = new ArrayList<>();
        updateStudentStatuses(registrations, graduated, eliminated);

        // 4. el período pasa a CLOSED
        period.setStatus(PeriodStatus.CLOSED);
        period.setOpenMarker(null);
        academicPeriodRepository.save(period);

        AcademicPeriodResponseDTO periodDTO = new AcademicPeriodResponseDTO(
                period.getId(), period.getYear(), period.getSemester(),
                AcademicPeriodService.periodCode(period), period.getStatus());
        return new PeriodCloseResponseDTO(periodDTO, recordsCreated, graduated, eliminated);
    }

    // rechaza el cierre e informa las secciones con inscripciones sin nota
    private void validateAllGraded(List<CourseRegistration> registrations) {
        Map<Long, Integer> missingBySection = new LinkedHashMap<>();
        for (CourseRegistration registration : registrations) {
            if (registration.getGrade() == null) {
                missingBySection.merge(registration.getSectionId(), 1, Integer::sum);
            }
        }
        if (missingBySection.isEmpty()) {
            return;
        }

        Map<Long, String> subjectCodes = new HashMap<>();
        for (Subject subject : subjectRepository.findAllById(subjectIdsOfSections(missingBySection.keySet()))) {
            subjectCodes.put(subject.getId(), subject.getCode());
        }
        List<String> details = new ArrayList<>();
        for (Section section : sectionRepository.findAllById(missingBySection.keySet())) {
            details.add("sección " + section.getId() + " (" + subjectCodes.get(section.getSubjectId()) + "): "
                    + missingBySection.get(section.getId()) + " sin nota");
        }
        throw new IllegalStateException(
                "No se puede cerrar el período, hay secciones con notas pendientes: " + String.join("; ", details));
    }

    private Set<Long> subjectIdsOfSections(Set<Long> sectionIds) {
        Set<Long> subjectIds = new HashSet<>();
        for (Section section : sectionRepository.findAllById(sectionIds)) {
            subjectIds.add(section.getSubjectId());
        }
        return subjectIds;
    }

    private int createRecords(AcademicPeriod period, List<CourseRegistration> registrations) {
        Set<Long> subjectIds = new HashSet<>();
        for (CourseRegistration registration : registrations) {
            subjectIds.add(registration.getSubjectId());
        }
        Map<Long, Subject> subjects = new HashMap<>();
        for (Subject subject : subjectRepository.findAllById(subjectIds)) {
            subjects.put(subject.getId(), subject);
        }

        Set<Long> alreadyRecorded = new HashSet<>();
        for (AcademicRecord record : academicRecordRepository.findByAcademicPeriodId(period.getId())) {
            alreadyRecorded.add(record.getCourseRegistrationId());
        }

        String periodCode = AcademicPeriodService.periodCode(period);
        int created = 0;
        for (CourseRegistration registration : registrations) {
            CourseResult result = registration.getGrade().compareTo(PASSING_GRADE) >= 0
                    ? CourseResult.PASSED : CourseResult.FAILED;
            registration.setResult(result);
            courseRegistrationRepository.save(registration);

            if (alreadyRecorded.contains(registration.getId())) {
                continue;
            }
            Subject subject = subjects.get(registration.getSubjectId());
            AcademicRecord record = new AcademicRecord();
            record.setStudentId(registration.getStudentId());
            record.setSubjectId(subject.getId());
            record.setAcademicPeriodId(period.getId());
            record.setCourseRegistrationId(registration.getId());
            record.setSubjectCode(subject.getCode());
            record.setSubjectName(subject.getName());
            record.setSubjectSemester(subject.getSemester());
            record.setCredits(subject.getCredits());
            record.setPeriodCode(periodCode);
            record.setFinalGrade(registration.getGrade());
            record.setResult(result);
            academicRecordRepository.save(record);
            created++;
        }
        return created;
    }

    // ELIMINATED: reprobó la misma asignatura por segunda vez. GRADUATED: aprobó todas las del plan
    private void updateStudentStatuses(List<CourseRegistration> registrations,
                                       List<Long> graduated, List<Long> eliminated) {
        Set<Long> studentIds = new LinkedHashSet<>();
        for (CourseRegistration registration : registrations) {
            studentIds.add(registration.getStudentId());
        }

        for (Long studentId : studentIds) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + studentId));
            if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.ELIMINATED) {
                continue;
            }

            Set<Long> passedIds = new HashSet<>();
            Map<Long, Integer> failsBySubject = new HashMap<>();
            for (AcademicRecord record : academicRecordRepository.findByStudentId(studentId)) {
                if (record.getResult() == CourseResult.PASSED) {
                    passedIds.add(record.getSubjectId());
                } else {
                    failsBySubject.merge(record.getSubjectId(), 1, Integer::sum);
                }
            }

            boolean failedTwice = false;
            for (int fails : failsBySubject.values()) {
                if (fails >= FAILS_TO_BE_ELIMINATED) {
                    failedTwice = true;
                }
            }

            if (failedTwice) {
                student.setStatus(StudentStatus.ELIMINATED);
                studentRepository.save(student);
                eliminated.add(studentId);
                continue;
            }

            Set<Long> planSubjectIds = new HashSet<>();
            for (Subject subject : subjectRepository.findByStudyPlanId(student.getStudyPlanId())) {
                planSubjectIds.add(subject.getId());
            }
            if (!planSubjectIds.isEmpty() && passedIds.containsAll(planSubjectIds)) {
                student.setStatus(StudentStatus.GRADUATED);
                studentRepository.save(student);
                graduated.add(studentId);
            }
        }
    }
}
