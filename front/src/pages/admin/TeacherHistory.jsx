import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import teacherService from "../../services/teacher.service";
import TeachingHistoryTable from "../../components/TeachingHistoryTable";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const TeacherHistory = () => {
  const { id } = useParams();
  const [teacher, setTeacher] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    teacherService
      .get(id)
      .then((response) => setTeacher(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, [id]);

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Historial docente{teacher && `: ${teacher.fullName}`}</h2>
        <Link className="btn btn-outline-secondary" to="/teachers">
          Volver
        </Link>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <TeachingHistoryTable teacherId={id} />
    </div>
  );
};

export default TeacherHistory;
