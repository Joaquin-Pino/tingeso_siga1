import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { NAV_ITEMS } from "../utils/navigation";

const Home = () => {
  const { username, hasAnyRole } = useAuth();
  const items = NAV_ITEMS.filter((item) => hasAnyRole(item.roles));

  return (
    <div>
      <h1>SIGA IPT</h1>
      <p>Bienvenido/a, {username}.</p>
      <div className="row g-3">
        {items.map((item) => (
          <div className="col-md-4" key={item.to}>
            <Link to={item.to} className="card h-100 text-decoration-none">
              <div className="card-body">
                <h5 className="card-title">{item.label}</h5>
                <p className="card-text text-muted">{item.description}</p>
              </div>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Home;
