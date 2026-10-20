// fuente única de los accesos por rol: la usan la Navbar y el Home.
// Las rutas se agregan aquí a medida que se implementan las vistas.
export const NAV_ITEMS = [
  { to: "/students", label: "Estudiantes", description: "Registro, estado e historial de estudiantes", roles: ["ADMIN"] },
  { to: "/teachers", label: "Docentes", description: "Registro y estado de docentes", roles: ["ADMIN"] },
  { to: "/careers", label: "Carreras", description: "Carreras, planes de estudio y asignaturas", roles: ["ADMIN"] },
  { to: "/periods", label: "Períodos", description: "Períodos, oferta de secciones, matrícula y cierre", roles: ["ADMIN"] },
];
