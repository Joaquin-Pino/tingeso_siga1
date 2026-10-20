import { useEffect, useState } from "react";
import studentService from "../services/student.service";
import { getErrorMessage } from "../utils/errors";

// el back resuelve la persona a partir del sub del JWT (keycloakId)
const useCurrentStudent = () => {
  const [student, setStudent] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    studentService
      .getMe()
      .then((response) => setStudent(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, []);

  return { student, error };
};

export default useCurrentStudent;
