// fuente única de los accesos por rol: la usan la Navbar y el Home.
// Las rutas se agregan aquí a medida que se implementan las vistas.
export const NAV_ITEMS = [
  { to: "/students", label: "Estudiantes", description: "Registro, estado e historial de estudiantes", roles: ["ADMIN"] },
  { to: "/teachers", label: "Docentes", description: "Registro y estado de docentes", roles: ["ADMIN"] },
  { to: "/careers", label: "Carreras", description: "Carreras, planes de estudio y asignaturas", roles: ["ADMIN"] },
  { to: "/periods", label: "Períodos", description: "Períodos, oferta de secciones, matrícula y cierre", roles: ["ADMIN"] },
  { to: "/study-plans", label: "Planes de estudio", description: "Crear planes de estudio", roles: ["ADMIN"] },
  { to: "/subjects/new", label: "Nueva asignatura", description: "Crear una asignatura en un plan de estudios", roles: ["ADMIN"] },
  { to: "/my-sections", label: "Mis secciones", description: "Secciones a cargo y registro de notas", roles: ["TEACHER"] },
  { to: "/my-profile", label: "Mi perfil", description: "Datos, plan de estudios y promedio", roles: ["STUDENT"] },
  { to: "/my-plan", label: "Mi plan", description: "Asignaturas del plan por semestre, prerrequisitos y avance", roles: ["STUDENT"] },
  { to: "/my-history", label: "Mi historial", description: "Asignaturas cursadas, notas y resultados", roles: ["STUDENT"] },
  { to: "/offering", label: "Oferta", description: "Secciones disponibles en el período abierto", roles: ["STUDENT"] },
  { to: "/registration", label: "Inscribir", description: "Inscripción de asignaturas del período", roles: ["STUDENT"] },
  { to: "/my-registrations", label: "Mis inscripciones", description: "Inscripciones y anulaciones", roles: ["STUDENT"] },
];
