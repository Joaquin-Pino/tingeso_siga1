import { useEffect, useState } from "react";
import teacherService from "../services/teacher.service";
import ErrorAlert from "./ErrorAlert";
import { getErrorMessage } from "../utils/errors";

// historial docente: secciones impartidas en períodos ya cerrados (snapshot tomado en el cierre)
const TeachingHistoryTable = ({ teacherId }) => {
  const [history, setHistory] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    teacherService
      .getTeachingHistory(teacherId)
      .then((response) => setHistory(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, [teacherId]);

  return (
    <div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <table className="table table-striped">
        <thead>
          <tr>
            <th>Período</th>
            <th>Código</th>
            <th>Asignatura</th>
            <th>Sem.</th>
            <th>Inscritos</th>
          </tr>
        </thead>
        <tbody>
          {history.map((r) => (
            <tr key={r.id}>
              <td>{r.periodCode}</td>
              <td>{r.subjectCode}</td>
              <td>{r.subjectName}</td>
              <td>{r.subjectSemester}</td>
              <td>{r.enrolledCount}</td>
            </tr>
          ))}
          {history.length === 0 && (
            <tr>
              <td colSpan="5" className="text-muted">
                Sin secciones en períodos cerrados.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default TeachingHistoryTable;
