package cl.joaquin.siga.Repositories.University;

import cl.joaquin.siga.Entities.University.Career;
import cl.joaquin.siga.Entities.University.CareerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Long> {
    boolean existsByIdAndStatus(Long id, CareerStatus status);
}
