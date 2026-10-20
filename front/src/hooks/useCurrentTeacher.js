import { useEffect, useState } from "react";
import teacherService from "../services/teacher.service";
import { getErrorMessage } from "../utils/errors";

// el back resuelve la persona a partir del sub del JWT (keycloakId)
const useCurrentTeacher = () => {
  const [teacher, setTeacher] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    teacherService
      .getMe()
      .then((response) => setTeacher(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, []);

  return { teacher, error };
};

export default useCurrentTeacher;
