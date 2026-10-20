package cl.joaquin.siga.Services;

import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock TeacherRepository teacherRepository;
    @InjectMocks CurrentUserService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(subject, "n/a");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Student student(String keycloakId) {
        Student student = new Student();
        student.setKeycloakId(keycloakId);
        return student;
    }

    private Teacher teacher(String keycloakId) {
        Teacher teacher = new Teacher();
        teacher.setKeycloakId(keycloakId);
        return teacher;
    }

    @Test
    void isStudent_ownId_returnsTrue() {
        authenticateAs("kc-1");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student("kc-1")));

        assertTrue(service.isStudent(1L));
    }

    @Test
    void isStudent_otherStudentsId_returnsFalse() {
        authenticateAs("kc-1");
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student("kc-2")));

        assertFalse(service.isStudent(2L));
    }

    @Test
    void isStudent_unknownStudent_returnsFalse() {
        authenticateAs("kc-1");
        when(studentRepository.findById(9L)).thenReturn(Optional.empty());

        assertFalse(service.isStudent(9L));
    }

    @Test
    void isStudent_noAuthentication_returnsFalse() {
        assertFalse(service.isStudent(1L));
    }

    @Test
    void isTeacher_ownId_returnsTrue() {
        authenticateAs("kc-1");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher("kc-1")));

        assertTrue(service.isTeacher(1L));
    }

    @Test
    void isTeacher_otherTeachersId_returnsFalse() {
        authenticateAs("kc-1");
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher("kc-2")));

        assertFalse(service.isTeacher(2L));
    }

    @Test
    void isTeacher_noAuthentication_returnsFalse() {
        assertFalse(service.isTeacher(1L));
    }
}
