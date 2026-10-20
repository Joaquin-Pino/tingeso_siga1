// fuente única de los accesos por rol: la usan la Navbar y el Home.
// Las rutas se agregan aquí a medida que se implementan las vistas.
export const NAV_ITEMS = [
  { to: "/students", label: "Estudiantes", description: "Registro, estado e historial de estudiantes", roles: ["ADMIN"] },
];
