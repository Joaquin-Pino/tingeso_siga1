import { useAuth } from "../context/AuthContext";
import Forbidden from "../pages/Forbidden";

const PrivateRoute = ({ element, rolesAllowed }) => {
  const { hasAnyRole } = useAuth();

  if (rolesAllowed && !hasAnyRole(rolesAllowed)) {
    return <Forbidden />;
  }
  return element;
};

export default PrivateRoute;
