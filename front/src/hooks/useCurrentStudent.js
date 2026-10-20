import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import studentService from "../services/student.service";
import { getErrorMessage } from "../utils/errors";

// TEMPORAL: igual que useCurrentTeacher, hasta que el back exponga GET /api/students/me.
const useCurrentStudent = () => {
  const { email } = useAuth();
  const [student, setStudent] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    studentService
      .getAll()
      .then((response) => {
        for (const s of response.data) {
          if (s.email.toLowerCase() === (email || "").toLowerCase()) {
            setStudent(s);
            return;
          }
        }
        setError("No existe un estudiante registrado con el correo de tu cuenta");
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, [email]);

  return { student, error };
};

export default useCurrentStudent;
