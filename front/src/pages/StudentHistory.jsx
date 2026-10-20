import { useEffect, useState } from "react";
import studentService from "../services/student.service";
import useCurrentStudent from "../hooks/useCurrentStudent";
import StatusBadge from "../components/StatusBadge";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";

const StudentHistory = () => {
  const { student, error: studentError } = useCurrentStudent();
  const [history, setHistory] = useState([]);
  const [average, setAverage] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!student) return;
    const fail = (e) => setError(getErrorMessage(e));
    studentService
      .getAcademicHistory(student.id)
      .then((response) => setHistory(response.data))
      .catch(fail);
    studentService
      .getAcademicAverage(student.id)
      .then((response) => setAverage(response.data))
      .catch(fail);
  }, [student]);

  return (
    <div>
      <h2>Mi historial académico</h2>
      <ErrorAlert message={studentError || error} onClose={() => setError("")} />
      {average && (
        <p>
          Promedio ponderado: <strong>{average.average ?? "sin historial"}</strong> · {average.subjectsCount} asignaturas ·{" "}
          {average.totalCredits} SCT
        </p>
      )}
      <table className="table table-striped">
        <thead>
          <tr>
            <th>Período</th>
            <th>Código</th>
            <th>Asignatura</th>
            <th>Sem.</th>
            <th>SCT</th>
            <th>Nota</th>
            <th>Resultado</th>
          </tr>
        </thead>
        <tbody>
          {history.map((r) => (
            <tr key={r.id}>
              <td>{r.periodCode}</td>
              <td>{r.subjectCode}</td>
              <td>{r.subjectName}</td>
              <td>{r.subjectSemester}</td>
              <td>{r.credits}</td>
              <td>{r.finalGrade}</td>
              <td>
                <StatusBadge status={r.result} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default StudentHistory;
