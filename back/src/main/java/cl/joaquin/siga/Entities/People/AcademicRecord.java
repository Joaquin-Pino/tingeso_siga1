package cl.joaquin.siga.Entities.People;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "academicRecord")
public class AcademicRecord {
    //TODO: implement XD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private Long subjectId;

    private Long academicPeriodId;

    private Long academicRecordId;

    // ver como continuar esto
}
