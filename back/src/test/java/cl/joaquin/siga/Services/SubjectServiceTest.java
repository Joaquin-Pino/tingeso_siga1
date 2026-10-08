package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.SubjectDTO.SubjectCreateDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectResponseDTO;
import cl.joaquin.siga.DTOs.SubjectDTO.SubjectUpdateDTO;
import cl.joaquin.siga.Entities.University.Subject;
import cl.joaquin.siga.Entities.University.SubjectPrerequisite;
import cl.joaquin.siga.Entities.University.WeeklyHours;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.AcademicRecordRepository;
import cl.joaquin.siga.Repositories.University.CourseRegistrationRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
import cl.joaquin.siga.Repositories.University.SubjectPrerequisiteRepository;
import cl.joaquin.siga.Repositories.University.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    @Mock StudyPlanRepository studyPlanRepository;
    @Mock AcademicRecordRepository academicRecordRepository;
    @Mock CourseRegistrationRepository courseRegistrationRepository;
    @Mock SectionRepository sectionRepository;
    @InjectMocks SubjectService service;

    private static Subject subject(Long id, Long studyPlanId, String code, int semester) {
        Subject s = new Subject();
        s.setId(id);
        s.setStudyPlanId(studyPlanId);
        s.setCode(code);
        s.setName(code);
        s.setSemester(semester);
        s.setWeeklyHours(new WeeklyHours(2, 2, 0));
        s.setCredits(5);
        return s;
    }

    private static SubjectCreateDTO createDTO(int semester, int t, int e, int l, Set<Long> prerequisiteIds) {
        return new SubjectCreateDTO(1L, "TAP302", "Ingeniería de Software", semester, t, e, l, 5, prerequisiteIds);
    }

    private static SubjectUpdateDTO updateDTO(int semester, Set<Long> prerequisiteIds) {
        return new SubjectUpdateDTO("TAP302", "Ingeniería de Software", semester, 2, 2, 0, 5, prerequisiteIds);
    }

    private void planExistsAndCodeFree() {
        when(studyPlanRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP302")).thenReturn(false);
    }

    @Test
    void createSubject_valid_savesSubjectAndPrerequisites() {
        planExistsAndCodeFree();
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject(10L, 1L, "TAP201", 2)));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> {
            Subject s = i.getArgument(0);
            s.setId(20L);
            return s;
        });

        SubjectResponseDTO result = service.createSubject(createDTO(3, 2, 2, 0, Set.of(10L)));

        assertEquals(20L, result.id());
        assertEquals(List.of(10L), result.prerequisiteIds());
        verify(subjectPrerequisiteRepository).save(argThat(row ->
                row.getSubjectId().equals(20L) && row.getPrerequisiteSubjectId().equals(10L)));
    }

    @Test
    void createSubject_studyPlanNotFound_throws() {
        when(studyPlanRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.createSubject(createDTO(1, 2, 2, 0, null)));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_duplicateCodeInPlan_throws() {
        when(studyPlanRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP302")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(1, 2, 2, 0, null)));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_weeklyHoursSumEight_throws() {
        planExistsAndCodeFree();

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(1, 4, 2, 2, null)));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_weeklyHoursSumSeven_ok() {
        planExistsAndCodeFree();
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.createSubject(createDTO(1, 3, 2, 2, null)));
    }

    @Test
    void createSubject_negativeHours_throws() {
        planExistsAndCodeFree();

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(1, -1, 2, 0, null)));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_negativeExerciseOrLabHours_throws() {
        planExistsAndCodeFree();

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(1, 2, -1, 0, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(1, 2, 2, -1, null)));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_moreThanThreePrerequisites_throws() {
        planExistsAndCodeFree();

        assertThrows(IllegalArgumentException.class,
                () -> service.createSubject(createDTO(4, 2, 2, 0, Set.of(1L, 2L, 3L, 4L))));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_prerequisiteNotFound_throws() {
        planExistsAndCodeFree();
        when(subjectRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createSubject(createDTO(3, 2, 2, 0, Set.of(10L))));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_prerequisiteFromOtherPlan_throws() {
        planExistsAndCodeFree();
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject(10L, 2L, "INF201", 2)));

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(3, 2, 2, 0, Set.of(10L))));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_prerequisiteSameSemester_throws() {
        planExistsAndCodeFree();
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject(10L, 1L, "TAP301", 3)));

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(3, 2, 2, 0, Set.of(10L))));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void updateSubject_selfAsPrerequisite_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));

        assertThrows(IllegalArgumentException.class, () -> service.updateSubject(20L, updateDTO(3, Set.of(20L))));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void updateSubject_movesBeforeDependent_throws() {
        // TAP302 (sem 3) es prerrequisito de TAP401 (sem 4); moverla al semestre 4 rompe la regla
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        SubjectPrerequisite dependency = new SubjectPrerequisite();
        dependency.setSubjectId(30L);
        dependency.setPrerequisiteSubjectId(20L);
        when(subjectPrerequisiteRepository.findByPrerequisiteSubjectId(20L)).thenReturn(List.of(dependency));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, 1L, "TAP401", 4)));

        assertThrows(IllegalArgumentException.class, () -> service.updateSubject(20L, updateDTO(4, Set.of())));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void updateSubject_replacesPrerequisites() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject(10L, 1L, "TAP201", 2)));
        when(subjectPrerequisiteRepository.findByPrerequisiteSubjectId(20L)).thenReturn(List.of());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        SubjectResponseDTO result = service.updateSubject(20L, updateDTO(3, Set.of(10L)));

        assertEquals(List.of(10L), result.prerequisiteIds());
        verify(subjectPrerequisiteRepository).deleteBySubjectId(20L);
        verify(subjectPrerequisiteRepository).save(any());
    }

    @Test
    void deleteSubject_inAcademicRecord_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(academicRecordRepository.existsBySubjectId(20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteSubject(20L));
        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteSubject_isPrerequisiteOfOther_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(academicRecordRepository.existsBySubjectId(20L)).thenReturn(false);
        when(subjectPrerequisiteRepository.existsByPrerequisiteSubjectId(20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteSubject(20L));
        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteSubject_unused_deletesSubjectAndPrerequisites() {
        Subject s = subject(20L, 1L, "TAP302", 3);
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(s));
        when(academicRecordRepository.existsBySubjectId(20L)).thenReturn(false);
        when(subjectPrerequisiteRepository.existsByPrerequisiteSubjectId(20L)).thenReturn(false);
        when(sectionRepository.existsBySubjectId(20L)).thenReturn(false);
        when(courseRegistrationRepository.existsBySubjectId(20L)).thenReturn(false);

        service.deleteSubject(20L);

        verify(subjectPrerequisiteRepository).deleteBySubjectId(20L);
        verify(subjectRepository).delete(s);
    }

    private static SubjectPrerequisite prerequisiteRow(Long subjectId, Long prerequisiteSubjectId) {
        SubjectPrerequisite row = new SubjectPrerequisite();
        row.setSubjectId(subjectId);
        row.setPrerequisiteSubjectId(prerequisiteSubjectId);
        return row;
    }

    // ---- consultas ----

    @Test
    void getById_includesPrerequisiteIdsAndHours() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(subjectPrerequisiteRepository.findBySubjectId(20L))
                .thenReturn(List.of(prerequisiteRow(20L, 10L), prerequisiteRow(20L, 11L)));

        SubjectResponseDTO result = service.getById(20L);

        assertEquals("TAP302", result.code());
        assertEquals(2, result.theoryHours());
        assertEquals(2, result.exerciseHours());
        assertEquals(0, result.labHours());
        assertEquals(List.of(10L, 11L), result.prerequisiteIds());
    }

    @Test
    void getById_notFound_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(20L));
    }

    @Test
    void getAllStudyPlanSubjects_mapsEachSubjectWithItsPrerequisites() {
        when(studyPlanRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.findByStudyPlanId(1L))
                .thenReturn(List.of(subject(10L, 1L, "TAP201", 2), subject(20L, 1L, "TAP302", 3)));
        when(subjectPrerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());
        when(subjectPrerequisiteRepository.findBySubjectId(20L)).thenReturn(List.of(prerequisiteRow(20L, 10L)));

        List<SubjectResponseDTO> result = service.getAllStudyPlanSubjects(1L);

        assertEquals(2, result.size());
        assertEquals(List.of(), result.get(0).prerequisiteIds());
        assertEquals(List.of(10L), result.get(1).prerequisiteIds());
    }

    @Test
    void getAllStudyPlanSubjects_planNotFound_throws() {
        when(studyPlanRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getAllStudyPlanSubjects(1L));
    }

    // ---- creación ----

    @Test
    void createSubject_withoutPrerequisites_returnsEmptyList() {
        planExistsAndCodeFree();
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        SubjectResponseDTO result = service.createSubject(createDTO(1, 2, 2, 0, null));

        assertEquals(List.of(), result.prerequisiteIds());
        assertEquals(1, result.semester());
        verify(subjectPrerequisiteRepository, never()).save(any());
    }

    @Test
    void createSubject_prerequisiteLaterSemester_throws() {
        planExistsAndCodeFree();
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject(10L, 1L, "TAP401", 4)));

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(createDTO(3, 2, 2, 0, Set.of(10L))));
        verify(subjectRepository, never()).save(any());
    }

    // ---- edición ----

    @Test
    void updateSubject_notFound_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateSubject(20L, updateDTO(3, Set.of())));
    }

    @Test
    void updateSubject_codeTakenInPlan_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP300", 3)));
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP302")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.updateSubject(20L, updateDTO(3, Set.of())));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void updateSubject_newFreeCode_isApplied() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP300", 3)));
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP302")).thenReturn(false);
        when(subjectPrerequisiteRepository.findByPrerequisiteSubjectId(20L)).thenReturn(List.of());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        SubjectResponseDTO result = service.updateSubject(20L, updateDTO(3, Set.of()));

        assertEquals("TAP302", result.code());
    }

    @Test
    void updateSubject_sameCode_skipsUniquenessCheck() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(subjectPrerequisiteRepository.findByPrerequisiteSubjectId(20L)).thenReturn(List.of());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        service.updateSubject(20L, updateDTO(3, null));

        verify(subjectRepository, never()).existsByStudyPlanIdAndCode(any(), any());
    }

    @Test
    void updateSubject_invalidWeeklyHours_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));

        assertThrows(IllegalArgumentException.class, () -> service.updateSubject(20L,
                new SubjectUpdateDTO("TAP302", "Ingeniería de Software", 3, 4, 4, 0, 5, Set.of())));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void updateSubject_movesEarlierWhileDependentStaysLater_ok() {
        // TAP302 (sem 3) es prerrequisito de TAP401 (sem 4); moverla al semestre 2 mantiene la regla
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(subjectPrerequisiteRepository.findByPrerequisiteSubjectId(20L)).thenReturn(List.of(prerequisiteRow(30L, 20L)));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, 1L, "TAP401", 4)));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        SubjectResponseDTO result = service.updateSubject(20L, updateDTO(2, Set.of()));

        assertEquals(2, result.semester());
    }

    @Test
    void updateSubject_flushesDeleteBeforeReinsertingPrerequisites() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject(10L, 1L, "TAP201", 2)));
        when(subjectPrerequisiteRepository.findByPrerequisiteSubjectId(20L)).thenReturn(List.of());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        service.updateSubject(20L, updateDTO(3, Set.of(10L)));

        InOrder order = inOrder(subjectPrerequisiteRepository);
        order.verify(subjectPrerequisiteRepository).deleteBySubjectId(20L);
        order.verify(subjectPrerequisiteRepository).flush();
        order.verify(subjectPrerequisiteRepository).save(any());
    }

    // ---- eliminación ----

    @Test
    void deleteSubject_notFound_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteSubject(20L));
    }

    @Test
    void deleteSubject_withSections_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(sectionRepository.existsBySubjectId(20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteSubject(20L));
        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteSubject_withRegistrations_throws() {
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, 1L, "TAP302", 3)));
        when(sectionRepository.existsBySubjectId(20L)).thenReturn(false);
        when(courseRegistrationRepository.existsBySubjectId(20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteSubject(20L));
        verify(subjectRepository, never()).delete(any());
    }
}
