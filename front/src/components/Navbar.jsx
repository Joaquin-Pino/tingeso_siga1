import { NavLink, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { NAV_ITEMS } from "../utils/navigation";

const APP_ROLES = ["ADMIN", "TEACHER", "STUDENT"];

const Navbar = () => {
  const { username, roles, hasAnyRole, logout } = useAuth();

  // Keycloak agrega roles propios (default-roles-*, offline_access, uma_authorization) que no son de la app
  const appRoles = [];
  for (const role of roles) {
    if (APP_ROLES.includes(role)) appRoles.push(role);
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
      <div className="container">
        <Link className="navbar-brand" to="/home">
          SIGA IPT
        </Link>
        <ul className="navbar-nav me-auto">
          {NAV_ITEMS.filter((item) => hasAnyRole(item.roles)).map((item) => (
            <li className="nav-item" key={item.to}>
              <NavLink className="nav-link" to={item.to}>
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
        <span className="navbar-text me-3">
          {username} ({appRoles.join(", ")})
        </span>
        <button className="btn btn-outline-light btn-sm" onClick={logout}>
          Salir
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
