package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodCreateDTO;
import cl.joaquin.siga.DTOs.AcademicPeriodDTO.AcademicPeriodResponseDTO;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Entities.University.Semester;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import cl.joaquin.siga.Repositories.University.SectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceTest {

    @Mock AcademicPeriodRepository academicPeriodRepository;
    @Mock SectionRepository sectionRepository;
    @Mock PeriodEnrollmentRepository periodEnrollmentRepository;
    @InjectMocks AcademicPeriodService service;

    private static AcademicPeriod period(int year, Semester semester, PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(1L);
        period.setYear(year);
        period.setSemester(semester);
        period.setStatus(status);
        return period;
    }

    @Test
    void getAll_mapsAllPeriods() {
        when(academicPeriodRepository.findAll()).thenReturn(List.of(
                period(2026, Semester.FIRST, PeriodStatus.CLOSED),
                period(2026, Semester.SECOND, PeriodStatus.OPEN)));

        List<AcademicPeriodResponseDTO> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("2026-2", result.get(1).code());
    }

    @Test
    void getById_returnsPeriodWithDerivedCode() {
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(2027, Semester.FIRST, PeriodStatus.OPEN)));

        AcademicPeriodResponseDTO result = service.getById(1L);

        assertEquals("2027-1", result.code());
        assertEquals(Semester.FIRST, result.semester());
    }

    @Test
    void getById_notFound_throws() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void createPeriod_isOpen() {
        when(academicPeriodRepository.save(any(AcademicPeriod.class))).thenAnswer(i -> i.getArgument(0));

        AcademicPeriodResponseDTO result = service.createPeriod(new AcademicPeriodCreateDTO(2027, Semester.SECOND));

        assertEquals(PeriodStatus.OPEN, result.status());
        assertEquals("2027-2", result.code());
    }

    @Test
    void createPeriod_duplicate_throws() {
        when(academicPeriodRepository.existsByYearAndSemester(2027, Semester.FIRST)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createPeriod(new AcademicPeriodCreateDTO(2027, Semester.FIRST)));
        verify(academicPeriodRepository, never()).save(any());
    }

    @Test
    void deletePeriod_unused_deletes() {
        AcademicPeriod period = period(2027, Semester.FIRST, PeriodStatus.OPEN);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        service.deletePeriod(1L);

        verify(academicPeriodRepository).delete(period);
    }

    @Test
    void deletePeriod_withSections_throws() {
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(2027, Semester.FIRST, PeriodStatus.OPEN)));
        when(sectionRepository.existsByAcademicPeriodId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deletePeriod(1L));
        verify(academicPeriodRepository, never()).delete(any());
    }

    @Test
    void deletePeriod_withEnrollments_throws() {
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(2027, Semester.FIRST, PeriodStatus.OPEN)));
        when(periodEnrollmentRepository.existsByAcademicPeriodId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deletePeriod(1L));
        verify(academicPeriodRepository, never()).delete(any());
    }
}
