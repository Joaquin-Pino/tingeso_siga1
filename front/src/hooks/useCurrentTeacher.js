import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import teacherService from "../services/teacher.service";
import { getErrorMessage } from "../utils/errors";

// TEMPORAL: el back aún no expone GET /api/teachers/me (no valida el JWT todavía),
// así que se resuelve el docente cruzando el correo del token con el listado.
// Cuando exista /me, solo cambia el cuerpo de este efecto.
const useCurrentTeacher = () => {
  const { email } = useAuth();
  const [teacher, setTeacher] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    teacherService
      .getAll()
      .then((response) => {
        for (const t of response.data) {
          if (t.email.toLowerCase() === (email || "").toLowerCase()) {
            setTeacher(t);
            return;
          }
        }
        setError("No existe un docente registrado con el correo de tu cuenta");
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, [email]);

  return { teacher, error };
};

export default useCurrentTeacher;
