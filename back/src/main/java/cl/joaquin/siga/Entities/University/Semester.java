package cl.joaquin.siga.Entities.University;

import lombok.Getter;

@Getter
public enum Semester {
    FIRST(1),
    SECOND(2);

    private final int number;

    Semester(int number) {
        this.number = number;
    }
}
