import { createContext, useContext, useEffect, useRef, useState } from "react";
import keycloak from "../services/keycloak";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [initialized, setInitialized] = useState(false);
  const started = useRef(false);

  useEffect(() => {
    // StrictMode ejecuta los efectos dos veces; keycloak.init solo puede llamarse una vez.
    if (started.current) return;
    started.current = true;
    // PKCE usa crypto.subtle, que el navegador solo da en contextos seguros (HTTPS o localhost).
    // En un despliegue por HTTP plano se desactiva.
    const pkceMethod = window.isSecureContext ? "S256" : false;
    keycloak
      .init({ onLoad: "login-required", pkceMethod })
      .then(() => setInitialized(true))
      .catch((error) => console.log("Error al iniciar Keycloak", error));
  }, []);

  if (!initialized) return <div className="container py-5">Cargando...</div>;

  const roles = keycloak.tokenParsed?.realm_access?.roles || [];
  const value = {
    authenticated: keycloak.authenticated,
    username: keycloak.tokenParsed?.preferred_username,
    roles,
    hasAnyRole: (allowed) => allowed.some((r) => roles.includes(r)),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);
