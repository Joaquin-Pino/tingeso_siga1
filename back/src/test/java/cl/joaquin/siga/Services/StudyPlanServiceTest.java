package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanCreateDTO;
import cl.joaquin.siga.DTOs.StudyPlanDTO.StudyPlanResponseDTO;
import cl.joaquin.siga.Entities.University.StudyPlan;
import cl.joaquin.siga.Entities.University.StudyPlanStatus;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock StudyPlanRepository studyPlanRepository;
    @Mock CareerRepository careerRepository;
    @Mock StudentRepository studentRepository;
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
    void deleteStudyPlan_withStudents_throws() {
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(new StudyPlan()));
        when(studentRepository.existsByStudyPlanId(5L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteStudyPlan(5L));
        verify(studyPlanRepository, never()).delete(any());
    }
}
