package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanUpdateDTO;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock StudyPlanRepository studyPlanRepository;
    @Mock CareerRepository careerRepository;
    @Mock StudentRepository studentRepository;
    @Mock SubjectRepository subjectRepository;
    @InjectMocks StudyPlanService service;

    @Test
    void createStudyPlan_deactivatesPreviousCurrent() {
        StudyPlan previous = new StudyPlan();
        previous.setStatus(StudyPlanStatus.CURRENT);
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.existsByCareerIdAndCode(1L, "2026.1")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.of(previous));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(i -> i.getArgument(0));

        StudyPlanResponseDTO result = service.createStudyPlan(new StudyPlanCreateDTO(1L, "2026.1"));

        assertEquals(StudyPlanStatus.NOT_CURRENT, previous.getStatus());
        assertEquals(StudyPlanStatus.CURRENT, result.status());
    }

    @Test
    void createStudyPlan_duplicateCode_throws() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.existsByCareerIdAndCode(1L, "2026.1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createStudyPlan(new StudyPlanCreateDTO(1L, "2026.1")));
        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void deleteStudyPlan_current_throws() {
        StudyPlan plan = new StudyPlan();
        plan.setStatus(StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan));

        assertThrows(IllegalStateException.class, () -> service.deleteStudyPlan(5L));
        verify(studyPlanRepository, never()).delete(any());
    }

    @Test
    void deleteStudyPlan_withStudents_throws() {
        StudyPlan plan = new StudyPlan();
        plan.setStatus(StudyPlanStatus.NOT_CURRENT);
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(studentRepository.existsByStudyPlanId(5L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteStudyPlan(5L));
        verify(studyPlanRepository, never()).delete(any());
    }

    @Test
    void deleteStudyPlan_withSubjects_throws() {
        StudyPlan plan = new StudyPlan();
        plan.setStatus(StudyPlanStatus.NOT_CURRENT);
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(studentRepository.existsByStudyPlanId(5L)).thenReturn(false);
        when(subjectRepository.existsByStudyPlanId(5L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteStudyPlan(5L));
        verify(studyPlanRepository, never()).delete(any());
    }

    private StudyPlan plan(Long id, Long careerId, String code, StudyPlanStatus status) {
        StudyPlan plan = new StudyPlan();
        plan.setId(id);
        plan.setCareerId(careerId);
        plan.setCode(code);
        plan.setStatus(status);
        return plan;
    }

    @Test
    void getAllCareerStudyPlan_mapsPlansOfCareer() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findByCareerId(1L)).thenReturn(List.of(
                plan(5L, 1L, "2020.1", StudyPlanStatus.NOT_CURRENT),
                plan(6L, 1L, "2026.1", StudyPlanStatus.CURRENT)));

        List<StudyPlanResponseDTO> result = service.getAllCareerStudyPlan(1L);

        assertEquals(2, result.size());
        assertEquals("2026.1", result.get(1).code());
        assertEquals(StudyPlanStatus.CURRENT, result.get(1).status());
    }

    @Test
    void getAllCareerStudyPlan_careerNotFound_throws() {
        when(careerRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getAllCareerStudyPlan(1L));
    }

    @Test
    void createStudyPlan_careerNotFound_throws() {
        when(careerRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.createStudyPlan(new StudyPlanCreateDTO(1L, "2026.1")));
        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void createStudyPlan_firstPlanOfCareer_isCurrent() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.empty());
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(i -> i.getArgument(0));

        StudyPlanResponseDTO result = service.createStudyPlan(new StudyPlanCreateDTO(1L, "2026.1"));

        assertEquals(StudyPlanStatus.CURRENT, result.status());
        assertEquals(1L, result.careerId());
        verify(studyPlanRepository, never()).saveAndFlush(any());
    }

    @Test
    void createStudyPlan_flushesPreviousBeforeSavingNew() {
        StudyPlan previous = plan(5L, 1L, "2020.1", StudyPlanStatus.CURRENT);
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.of(previous));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(i -> i.getArgument(0));

        service.createStudyPlan(new StudyPlanCreateDTO(1L, "2026.1"));

        // el índice único parcial exige que el anterior deje de ser vigente antes de insertar el nuevo
        InOrder order = inOrder(studyPlanRepository);
        order.verify(studyPlanRepository).saveAndFlush(previous);
        order.verify(studyPlanRepository).save(any(StudyPlan.class));
    }

    @Test
    void updateStudyPlan_changesCode() {
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan(5L, 1L, "2020.1", StudyPlanStatus.CURRENT)));
        when(studyPlanRepository.existsByCareerIdAndCode(1L, "2020.2")).thenReturn(false);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(i -> i.getArgument(0));

        StudyPlanResponseDTO result = service.updateStudyPlan(5L, new StudyPlanUpdateDTO("2020.2"));

        assertEquals("2020.2", result.code());
        assertEquals(StudyPlanStatus.CURRENT, result.status());
    }

    @Test
    void updateStudyPlan_sameCode_skipsUniquenessCheck() {
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan(5L, 1L, "2020.1", StudyPlanStatus.CURRENT)));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(i -> i.getArgument(0));

        service.updateStudyPlan(5L, new StudyPlanUpdateDTO("2020.1"));

        verify(studyPlanRepository, never()).existsByCareerIdAndCode(any(), any());
    }

    @Test
    void updateStudyPlan_codeTakenInCareer_throws() {
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan(5L, 1L, "2020.1", StudyPlanStatus.CURRENT)));
        when(studyPlanRepository.existsByCareerIdAndCode(1L, "2026.1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStudyPlan(5L, new StudyPlanUpdateDTO("2026.1")));
        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void updateStudyPlan_notFound_throws() {
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateStudyPlan(5L, new StudyPlanUpdateDTO("x")));
    }

    @Test
    void deleteStudyPlan_notCurrentAndUnused_deletes() {
        StudyPlan plan = plan(5L, 1L, "2020.1", StudyPlanStatus.NOT_CURRENT);
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan));

        service.deleteStudyPlan(5L);

        verify(studyPlanRepository).delete(plan);
    }

    @Test
    void deleteStudyPlan_notFound_throws() {
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteStudyPlan(5L));
    }
}
