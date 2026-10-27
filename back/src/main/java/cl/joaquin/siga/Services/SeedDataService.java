package cl.joaquin.siga.Services;

import cl.joaquin.siga.Config.SeedData;
import cl.joaquin.siga.Config.SeedData.CareerSeed;
import cl.joaquin.siga.Config.SeedData.SubjectSeed;
import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Carga inicial de las carreras, planes y asignaturas de SeedData. Pasa por los mismos Services que la API,
// así que los datos cumplen las mismas reglas. Todo o nada: una sola transacción
@Service
@RequiredArgsConstructor
public class SeedDataService {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);

    private final CareerRepository careerRepository;
    private final CareerService careerService;
    private final StudyPlanService studyPlanService;
    private final SubjectService subjectService;

    // solo carga si no hay ninguna carrera; devuelve false si no hizo nada
    @Transactional
    public boolean seedIfEmpty() {
        if (careerRepository.count() > 0) {
            return false;
        }

        int subjects = 0;
        for (CareerSeed career : SeedData.CAREERS) {
            Long careerId = careerService.createCareer(new CareerCreateDTO(
                    career.code(), career.name(), career.description(), career.startYear(), career.vacancies())).id();
            Long planId = studyPlanService.createStudyPlan(new StudyPlanCreateDTO(careerId, career.planCode())).id();

            Map<String, Long> subjectIdsByCode = new HashMap<>();
            for (SubjectSeed subject : career.subjects()) {
                subjectIdsByCode.put(subject.code(), createSubject(planId, subject, subjectIdsByCode));
                subjects++;
            }
        }
        log.info("Carga inicial: {} carreras y {} asignaturas", SeedData.CAREERS.size(), subjects);
        return true;
    }

    private Long createSubject(Long planId, SubjectSeed subject, Map<String, Long> subjectIdsByCode) {
        Set<Long> prerequisiteIds = new HashSet<>();
        for (String code : subject.prerequisiteCodes()) {
            prerequisiteIds.add(subjectIdsByCode.get(code));
        }
        return subjectService.createSubject(new SubjectCreateDTO(
                planId, subject.code(), subject.name(), subject.semester(), subject.theoryHours(),
                subject.exerciseHours(), subject.labHours(), subject.credits(), prerequisiteIds)).id();
    }
}
