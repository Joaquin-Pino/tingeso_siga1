package cl.joaquin.siga.Entities.University;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "section_schedule_block",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_section_schedule_block_section_day_module",
                columnNames = {"section_id", "day", "module"}
        )
)
public class SectionScheduleBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeekDay day;

    @Column(nullable = false)
    private Integer module;
}
