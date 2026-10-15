import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const Navbar = () => {
  const { username, roles, hasAnyRole, logout } = useAuth();

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
      <div className="container">
        <Link className="navbar-brand" to="/home">
          SIGA IPT
        </Link>
        <ul className="navbar-nav me-auto">
          {hasAnyRole(["ADMIN"]) && (
            <li className="nav-item">
              <Link className="nav-link" to="/students">
                Estudiantes
              </Link>
            </li>
          )}
        </ul>
        <span className="navbar-text me-3">
          {username} ({roles.join(", ")})
        </span>
        <button className="btn btn-outline-light btn-sm" onClick={logout}>
          Salir
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
