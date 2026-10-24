import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import academicPeriodService from "../../services/academicPeriod.service";
import enrollmentService from "../../services/enrollment.service";
import studentService from "../../services/student.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const PeriodEnrollments = () => {
  const { id } = useParams();
  const [period, setPeriod] = useState(null);
  const [enrollments, setEnrollments] = useState([]);
  const [students, setStudents] = useState([]);
  const [studentId, setStudentId] = useState("");
  const [error, setError] = useState("");

  const fail = (e) => setError(getErrorMessage(e));

  const loadEnrollments = () => {
    enrollmentService
      .getAll(id)
      .then((response) => setEnrollments(response.data))
      .catch(fail);
  };

  useEffect(() => {
    academicPeriodService
      .get(id)
      .then((response) => setPeriod(response.data))
      .catch(fail);
    studentService
      .getAll()
      .then((response) => setStudents(response.data))
      .catch(fail);
    loadEnrollments();
  }, [id]);

  const enroll = (event) => {
    event.preventDefault();
    setError("");
    enrollmentService
      .enroll(id, Number(studentId))
      .then(() => {
        setStudentId("");
        loadEnrollments();
      })
      .catch(fail);
  };

  const studentsById = {};
  for (const s of students) studentsById[s.id] = s;

  // solo estudiantes regulares que aún no están matriculados en el período
  const enrolledIds = new Set();
  for (const e of enrollments) enrolledIds.add(e.studentId);
  const candidates = [];
  for (const s of students) {
    if (s.status === "REGULAR" && !enrolledIds.has(s.id)) candidates.push(s);
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>
          Matrícula {period ? period.code : ""} {period && <StatusBadge status={period.status} />}
        </h2>
        <Link className="btn btn-outline-secondary" to="/periods">
          Volver
        </Link>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />

      <form className="row g-2 align-items-end mb-4" onSubmit={enroll}>
        <div className="col-md-6">
          <label className="form-label" htmlFor="student">
            Matricular estudiante
          </label>
          <select id="student" className="form-select" value={studentId} onChange={(e) => setStudentId(e.target.value)} required>
            <option value="">Seleccione...</option>
            {candidates.map((s) => (
              <option key={s.id} value={s.id}>
                {s.run} - {s.fullName}
              </option>
            ))}
          </select>
        </div>
        <div className="col-auto">
          <button className="btn btn-primary" type="submit">
            Matricular
          </button>
        </div>
      </form>

      <table className="table table-striped">
        <thead>
          <tr>
            <th>RUN</th>
            <th>Estudiante</th>
            <th>Fecha de matrícula</th>
          </tr>
        </thead>
        <tbody>
          {enrollments.map((e) => (
            <tr key={e.id}>
              <td>{studentsById[e.studentId]?.run}</td>
              <td>{studentsById[e.studentId]?.fullName}</td>
              <td>{e.enrollmentDate}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default PeriodEnrollments;
