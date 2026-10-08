package cl.joaquin.siga.Services;

import cl.joaquin.siga.DTOs.CareerDTO.CareerCreateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerResponseDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerStatusUpdateDTO;
import cl.joaquin.siga.DTOs.CareerDTO.CareerUpdateDTO;
import cl.joaquin.siga.Entities.University.Career;
import cl.joaquin.siga.Entities.University.CareerStatus;
import cl.joaquin.siga.Exceptions.NotFoundException;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.University.CareerRepository;
import cl.joaquin.siga.Repositories.University.StudyPlanRepository;
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
class CareerServiceTest {

    @Mock CareerRepository careerRepository;
    @Mock StudyPlanRepository studyPlanRepository;
    @Mock StudentRepository studentRepository;
    @InjectMocks CareerService service;

    @Test
    void createCareer_assignsActiveStatus() {
        when(careerRepository.existsByCode("INF")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(i -> i.getArgument(0));

        CareerResponseDTO result = service.createCareer(new CareerCreateDTO("INF", "Informática", "desc", 2020, 50));

        assertEquals(CareerStatus.ACTIVE, result.status());
    }

    @Test
    void createCareer_duplicateCode_throws() {
        when(careerRepository.existsByCode("INF")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createCareer(new CareerCreateDTO("INF", "Informática", "desc", 2020, 50)));
        verify(careerRepository, never()).save(any());
    }

    @Test
    void deleteCareer_withStudents_throws() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(new Career()));
        when(studentRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteCareer(1L));
        verify(careerRepository, never()).delete(any());
    }

    @Test
    void deleteCareer_withStudyPlans_throws() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(new Career()));
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteCareer(1L));
        verify(careerRepository, never()).delete(any());
    }

    @Test
    void deleteCareer_withoutDependencies_deletes() {
        Career career = new Career();
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));

        service.deleteCareer(1L);

        verify(careerRepository).delete(career);
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(9L));
    }

    private Career existingCareer() {
        Career career = new Career();
        career.setId(1L);
        career.setCode("INF");
        career.setName("Informática");
        career.setDescription("desc");
        career.setStartYear(2020);
        career.setVacancies(50);
        career.setStatus(CareerStatus.ACTIVE);
        return career;
    }

    @Test
    void getAll_mapsAllCareers() {
        when(careerRepository.findAll()).thenReturn(List.of(existingCareer(), existingCareer()));

        assertEquals(2, service.getAll().size());
    }

    @Test
    void getById_returnsCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existingCareer()));

        CareerResponseDTO result = service.getById(1L);

        assertEquals("INF", result.code());
        assertEquals(CareerStatus.ACTIVE, result.status());
    }

    @Test
    void createCareer_copiesFieldsFromDto() {
        when(careerRepository.save(any(Career.class))).thenAnswer(i -> i.getArgument(0));

        CareerResponseDTO result = service.createCareer(new CareerCreateDTO("INF", "Informática", "desc", 2020, 50));

        assertEquals("INF", result.code());
        assertEquals("Informática", result.name());
        assertEquals("desc", result.description());
        assertEquals(2020, result.startYear());
        assertEquals(50, result.vacancies());
    }

    @Test
    void updateCareer_updatesFields_keepsCodeAndStatus() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existingCareer()));
        when(careerRepository.save(any(Career.class))).thenAnswer(i -> i.getArgument(0));

        CareerResponseDTO result = service.updateCareer(1L, new CareerUpdateDTO("Informática II", "otra", 2021, 60));

        assertEquals("INF", result.code());
        assertEquals(CareerStatus.ACTIVE, result.status());
        assertEquals("Informática II", result.name());
        assertEquals("otra", result.description());
        assertEquals(2021, result.startYear());
        assertEquals(60, result.vacancies());
    }

    @Test
    void updateCareer_notFound_throws() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateCareer(9L, new CareerUpdateDTO("x", "x", 2021, 1)));
        verify(careerRepository, never()).save(any());
    }

    @Test
    void updateStatus_changesStatus() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existingCareer()));
        when(careerRepository.save(any(Career.class))).thenAnswer(i -> i.getArgument(0));

        CareerResponseDTO result = service.updateStatus(1L, new CareerStatusUpdateDTO(CareerStatus.INACTIVE));

        assertEquals(CareerStatus.INACTIVE, result.status());
    }

    @Test
    void updateStatus_notFound_throws() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateStatus(9L, new CareerStatusUpdateDTO(CareerStatus.INACTIVE)));
    }

    @Test
    void deleteCareer_notFound_throws() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteCareer(9L));
        verify(careerRepository, never()).delete(any());
    }
}
