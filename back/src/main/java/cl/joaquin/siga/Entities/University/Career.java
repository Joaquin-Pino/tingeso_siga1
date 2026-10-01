package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "career",
    uniqueConstraints = @UniqueConstraint(name = "uk_career_code", columnNames = {"code"}
    )
)
public class Career {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private CareerStatus status;

    @Column(nullable = false)
    private Integer startingYear;

    @Column(nullable = false)
    private Integer vacancy;

}
