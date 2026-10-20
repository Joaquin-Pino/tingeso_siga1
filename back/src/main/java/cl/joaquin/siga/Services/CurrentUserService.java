package cl.joaquin.siga.Services;

import cl.joaquin.siga.Entities.People.Student;
import cl.joaquin.siga.Entities.People.Teacher;
import cl.joaquin.siga.Repositories.People.StudentRepository;
import cl.joaquin.siga.Repositories.People.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

// pregunta de autorización "¿este id soy yo?" para los @PreAuthorize de los controllers.
// El sub del JWT se compara con el keycloakId de la persona (ver SIGA_CONTEXT §7).
@Service("currentUser")
@RequiredArgsConstructor
public class CurrentUserService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public boolean isStudent(Long studentId) {
        String subject = currentSubject();
        if (subject == null) {
            return false;
        }
        Optional<Student> student = studentRepository.findById(studentId);
        return student.isPresent() && subject.equals(student.get().getKeycloakId());
    }

    public boolean isTeacher(Long teacherId) {
        String subject = currentSubject();
        if (subject == null) {
            return false;
        }
        Optional<Teacher> teacher = teacherRepository.findById(teacherId);
        return teacher.isPresent() && subject.equals(teacher.get().getKeycloakId());
    }

    private static String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
