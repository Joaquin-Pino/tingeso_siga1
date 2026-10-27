package cl.joaquin.siga.Config;

import java.util.List;

// Apéndice A del enunciado: carreras, planes de estudio vigentes y asignaturas que se cargan al arrancar
// (ver SeedDataService). Los prerrequisitos se indican por código y van siempre antes de la asignatura que los exige
public final class SeedData {

    private SeedData() {
    }

    public record CareerSeed(String code, String name, String description, int startYear, int vacancies,
                             String planCode, List<SubjectSeed> subjects) {
    }

    // hours = teoría, ejercicios y laboratorio (TEL)
    public record SubjectSeed(int semester, String code, String name, int theoryHours, int exerciseHours,
                              int labHours, int credits, List<String> prerequisiteCodes) {
    }

    private static SubjectSeed subject(int semester, String code, String name, String hours, int credits,
                                       String... prerequisiteCodes) {
        String[] tel = hours.split("-");
        return new SubjectSeed(semester, code, name, Integer.parseInt(tel[0]), Integer.parseInt(tel[1]),
                Integer.parseInt(tel[2]), credits, List.of(prerequisiteCodes));
    }

    public static final List<CareerSeed> CAREERS = List.of(
            new CareerSeed("10450", "Técnico Analista Programador",
                    "Carrera técnica de 4 semestres, plan de estudios 2021.3",
                    2000, 50, "2021.3", List.of(
                    subject(1, "TAP101", "Fundamentos de Programación", "2-2-2", 6),
                    subject(1, "TAP102", "Matemática Aplicada", "2-2-0", 5),
                    subject(1, "TAP103", "Fundamentos de Computación", "2-0-2", 4),
                    subject(1, "TAP104", "Bases de Datos I", "2-0-2", 5),
                    subject(1, "TAP105", "Comunicación Efectiva", "2-2-0", 4),
                    subject(1, "TAP106", "Inglés Técnico I", "2-2-0", 4),
                    subject(2, "TAP201", "Programación Orientada a Objetos", "2-2-2", 6, "TAP101"),
                    subject(2, "TAP202", "Estructuras de Datos", "2-2-2", 6, "TAP101"),
                    subject(2, "TAP203", "Bases de Datos II", "2-0-2", 5, "TAP104"),
                    subject(2, "TAP204", "Desarrollo Web I", "2-0-4", 6, "TAP101"),
                    subject(2, "TAP205", "Sistemas Operativos", "2-0-2", 4, "TAP103"),
                    subject(2, "TAP206", "Inglés Técnico II", "2-2-0", 3, "TAP106"),
                    subject(3, "TAP301", "Desarrollo Web II", "2-0-4", 6, "TAP201", "TAP204"),
                    subject(3, "TAP302", "Ingeniería de Software", "2-2-0", 5, "TAP201"),
                    subject(3, "TAP303", "Desarrollo de Aplicaciones Móviles", "2-0-4", 6, "TAP201"),
                    subject(3, "TAP304", "Administración de Bases de Datos", "2-0-2", 5, "TAP203"),
                    subject(3, "TAP305", "Redes y Comunicaciones", "2-0-2", 4, "TAP205"),
                    subject(3, "TAP306", "Taller de Programación", "0-0-4", 4, "TAP201", "TAP202"),
                    subject(4, "TAP401", "Arquitectura y Desarrollo de Software", "2-0-4", 6, "TAP301", "TAP302"),
                    subject(4, "TAP402", "Desarrollo de Aplicaciones Cloud", "2-0-4", 6, "TAP301"),
                    subject(4, "TAP403", "Seguridad de Aplicaciones", "2-0-2", 4, "TAP301", "TAP305"),
                    subject(4, "TAP404", "Calidad y Pruebas de Software", "2-0-2", 5, "TAP302"),
                    subject(4, "TAP405", "Proyecto de Desarrollo de Software", "0-0-6", 6, "TAP301", "TAP302", "TAP306"),
                    subject(4, "TAP406", "Emprendimiento y Gestión de Proyectos", "2-2-0", 3, "TAP302"))),
            new CareerSeed("10451", "Técnico en Análisis de Datos",
                    "Carrera técnica de 4 semestres, plan de estudios 2022.3",
                    2018, 50, "2022.3", List.of(
                    subject(1, "TAD101", "Fundamentos de Programación", "2-2-2", 6),
                    subject(1, "TAD102", "Matemática Aplicada", "2-2-0", 5),
                    subject(1, "TAD103", "Introducción al Análisis de Datos", "2-0-2", 5),
                    subject(1, "TAD104", "Bases de Datos I", "2-0-2", 5),
                    subject(1, "TAD105", "Herramientas Computacionales", "0-0-4", 4),
                    subject(1, "TAD106", "Comunicación Efectiva", "2-2-0", 4),
                    subject(2, "TAD201", "Programación para Análisis de Datos", "2-0-4", 6, "TAD101"),
                    subject(2, "TAD202", "Probabilidad y Estadística", "2-2-2", 6, "TAD102"),
                    subject(2, "TAD203", "Bases de Datos II", "2-0-2", 5, "TAD104"),
                    subject(2, "TAD204", "Preparación y Calidad de Datos", "2-0-4", 5, "TAD103", "TAD101"),
                    subject(2, "TAD205", "Visualización de Datos", "2-0-2", 5, "TAD103"),
                    subject(2, "TAD206", "Inglés Técnico", "2-2-0", 3),
                    subject(3, "TAD301", "Estadística Aplicada", "2-2-2", 6, "TAD202"),
                    subject(3, "TAD302", "Minería de Datos", "2-0-4", 6, "TAD201", "TAD202"),
                    subject(3, "TAD303", "Inteligencia de Negocios", "2-0-4", 5, "TAD203", "TAD205"),
                    subject(3, "TAD304", "Integración y Procesamiento de Datos", "2-0-4", 5, "TAD201", "TAD203"),
                    subject(3, "TAD305", "Visualización Avanzada de Datos", "2-0-2", 4, "TAD205"),
                    subject(3, "TAD306", "Ética y Protección de Datos", "2-2-0", 4, "TAD103"),
                    subject(4, "TAD401", "Fundamentos de Machine Learning", "2-0-4", 6, "TAD301", "TAD302"),
                    subject(4, "TAD402", "Big Data y Procesamiento de Datos", "2-0-4", 5, "TAD304"),
                    subject(4, "TAD403", "Analítica de Negocios", "2-0-2", 5, "TAD301", "TAD303"),
                    subject(4, "TAD404", "Desarrollo de Soluciones de Datos", "2-0-4", 5, "TAD302", "TAD304"),
                    subject(4, "TAD405", "Proyecto de Análisis de Datos", "0-0-6", 6, "TAD302", "TAD303", "TAD305"),
                    subject(4, "TAD406", "Emprendimiento y Gestión de Proyectos", "2-2-0", 3))),
            new CareerSeed("10452", "Técnico en Ciberseguridad",
                    "Carrera técnica de 4 semestres, plan de estudios 2021.3",
                    2012, 50, "2021.3", List.of(
                    subject(1, "TCS101", "Fundamentos de Programación", "2-2-2", 6),
                    subject(1, "TCS102", "Fundamentos de Sistemas Computacionales", "2-0-2", 5),
                    subject(1, "TCS103", "Redes de Computadores I", "2-0-2", 5),
                    subject(1, "TCS104", "Sistemas Operativos", "2-0-2", 5),
                    subject(1, "TCS105", "Matemática Aplicada", "2-2-0", 5),
                    subject(1, "TCS106", "Comunicación Efectiva", "2-2-0", 4),
                    subject(2, "TCS201", "Programación para Ciberseguridad", "2-0-4", 5, "TCS101"),
                    subject(2, "TCS202", "Redes de Computadores II", "2-0-4", 6, "TCS103"),
                    subject(2, "TCS203", "Administración de Sistemas Linux", "2-0-4", 5, "TCS104"),
                    subject(2, "TCS204", "Fundamentos de Ciberseguridad", "2-2-2", 6, "TCS102", "TCS103"),
                    subject(2, "TCS205", "Bases de Datos", "2-0-2", 5, "TCS101"),
                    subject(2, "TCS206", "Inglés Técnico", "2-2-0", 3),
                    subject(3, "TCS301", "Seguridad de Redes", "2-0-4", 6, "TCS202", "TCS204"),
                    subject(3, "TCS302", "Seguridad de Sistemas Operativos", "2-0-4", 5, "TCS203", "TCS204"),
                    subject(3, "TCS303", "Criptografía Aplicada", "2-2-2", 5, "TCS204"),
                    subject(3, "TCS304", "Seguridad de Aplicaciones Web", "2-0-4", 5, "TCS201", "TCS204"),
                    subject(3, "TCS305", "Monitoreo y Detección de Amenazas", "2-0-4", 5, "TCS202", "TCS204"),
                    subject(3, "TCS306", "Legislación y Ética en Ciberseguridad", "2-2-0", 4, "TCS204"),
                    subject(4, "TCS401", "Ethical Hacking y Pruebas de Penetración", "2-0-4", 6, "TCS301", "TCS302"),
                    subject(4, "TCS402", "Gestión de Incidentes de Ciberseguridad", "2-0-4", 5, "TCS305"),
                    subject(4, "TCS403", "Seguridad en Cloud", "2-0-4", 5, "TCS301", "TCS302"),
                    subject(4, "TCS404", "Análisis Forense Digital", "2-0-4", 5, "TCS302"),
                    subject(4, "TCS405", "Proyecto de Ciberseguridad", "0-0-6", 6, "TCS301", "TCS302", "TCS305"),
                    subject(4, "TCS406", "Gestión de Riesgos de Ciberseguridad", "2-2-0", 3, "TCS204"))));
}
