package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "academic_period",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_period_year_semester",
                columnNames = {"year", "semester"}))
public class AcademicPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodStatus status;

    // TODO: mover a AcademicPeriodService cuando exista esa capa; no debe quedar lógica en la entidad.
    @Transient
    public String getCode() {
        return year + "-" + semester.getNumber();
    }
}
