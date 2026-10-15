package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentCreateDTO;
import cl.joaquin.siga.DTOs.PeriodEnrollmentDTO.PeriodEnrollmentResponseDTO;
import cl.joaquin.siga.Entities.University.AcademicPeriod;
import cl.joaquin.siga.Entities.University.PeriodEnrollment;
import cl.joaquin.siga.Entities.University.PeriodStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.AcademicPeriodRepository;
import cl.joaquin.siga.Repositories.University.PeriodEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodEnrollmentServiceTest {

    @Mock PeriodEnrollmentRepository periodEnrollmentRepository;
    @Mock AcademicPeriodRepository academicPeriodRepository;
    @Mock StudentRepository studentRepository;

    PeriodEnrollmentService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2027-03-01T12:00:00Z"), ZoneId.of("America/Santiago"));
        service = new PeriodEnrollmentService(periodEnrollmentRepository, academicPeriodRepository,
                studentRepository, clock);
    }

    private static AcademicPeriod period(PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(10L);
        period.setStatus(status);
        return period;
    }

    @Test
    void enroll_valid_savesWithTodayDate() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(studentRepository.existsById(3L)).thenReturn(true);
        when(periodEnrollmentRepository.save(any(PeriodEnrollment.class))).thenAnswer(i -> {
            PeriodEnrollment e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        PeriodEnrollmentResponseDTO result = service.enroll(10L, new PeriodEnrollmentCreateDTO(3L));

        assertEquals(LocalDate.of(2027, 3, 1), result.enrollmentDate());
        assertEquals(3L, result.studentId());
        assertEquals(10L, result.academicPeriodId());
    }

    @Test
    void enroll_periodNotFound_throwsNotFound() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.enroll(10L, new PeriodEnrollmentCreateDTO(3L)));
    }

    @Test
    void enroll_closedPeriod_isRejected() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.CLOSED)));

        assertThrows(IllegalStateException.class, () -> service.enroll(10L, new PeriodEnrollmentCreateDTO(3L)));
        verify(periodEnrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_studentNotFound_throwsNotFound() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(studentRepository.existsById(3L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.enroll(10L, new PeriodEnrollmentCreateDTO(3L)));
    }

    @Test
    void enroll_alreadyEnrolled_isBadRequest() {
        when(academicPeriodRepository.findById(10L)).thenReturn(Optional.of(period(PeriodStatus.OPEN)));
        when(studentRepository.existsById(3L)).thenReturn(true);
        when(periodEnrollmentRepository.existsByStudentIdAndAcademicPeriodId(3L, 10L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.enroll(10L, new PeriodEnrollmentCreateDTO(3L)));
        verify(periodEnrollmentRepository, never()).save(any());
    }

    @Test
    void getByPeriod_unknownPeriod_throwsNotFound() {
        when(academicPeriodRepository.existsById(10L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getByPeriod(10L));
    }

    @Test
    void getByPeriod_returnsEnrollments() {
        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setId(1L);
        enrollment.setStudentId(3L);
        enrollment.setAcademicPeriodId(10L);
        when(academicPeriodRepository.existsById(10L)).thenReturn(true);
        when(periodEnrollmentRepository.findByAcademicPeriodId(10L)).thenReturn(List.of(enrollment));

        assertEquals(1, service.getByPeriod(10L).size());
    }
}
