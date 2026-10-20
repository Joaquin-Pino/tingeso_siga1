import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import studentService from "../services/student.service";
import careerService from "../services/career.service";
import StatusBadge from "../components/StatusBadge";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { MANUAL_STUDENT_STATUSES } from "../utils/labels";

const StudentDetail = () => {
  const { id } = useParams();
  const [student, setStudent] = useState(null);
  const [history, setHistory] = useState([]);
  const [average, setAverage] = useState(null);
  const [careers, setCareers] = useState([]);
  const [plans, setPlans] = useState([]);
  const [status, setStatus] = useState("");
  const [careerId, setCareerId] = useState("");
  const [planId, setPlanId] = useState("");
  const [error, setError] = useState("");

  const fail = (e) => setError(getErrorMessage(e));

  const load = () => {
    studentService
      .get(id)
      .then((response) => {
        const s = response.data;
        setStudent(s);
        setStatus(s.status);
        setCareerId(s.careerId);
        setPlanId(s.studyPlanId);
        careerService
          .getStudyPlans(s.careerId)
          .then((r) => setPlans(r.data))
          .catch(fail);
      })
      .catch(fail);
    studentService
      .getAcademicHistory(id)
      .then((response) => setHistory(response.data))
      .catch(fail);
    studentService
      .getAcademicAverage(id)
      .then((response) => setAverage(response.data))
      .catch(fail);
  };

  useEffect(() => {
    load();
    careerService
      .getAll()
      .then((response) => setCareers(response.data))
      .catch(fail);
  }, [id]);

  const apply = (request) => {
    setError("");
    request.then(load).catch(fail);
  };

  if (!student) return <ErrorAlert message={error} />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center">
        <h2>
          {student.fullName} <StatusBadge status={student.status} />
        </h2>
        <div>
          <Link className="btn btn-outline-primary me-2" to={`/students/${id}/registrations`}>
            Inscripciones
          </Link>
          <Link className="btn btn-outline-secondary" to="/students">
            Volver
          </Link>
        </div>
      </div>
      <p className="text-muted">
        RUN {student.run} · {student.email}
      </p>
      <ErrorAlert message={error} onClose={() => setError("")} />

      <div className="card mb-4">
        <div className="card-body">
          <h5 className="card-title">Gestión administrativa</h5>
          <div className="row g-3 align-items-end">
            <div className="col-md-4">
              <label className="form-label" htmlFor="status">
                Estado
              </label>
              <div className="input-group">
                <select id="status" className="form-select" value={status} onChange={(e) => setStatus(e.target.value)}>
                  {MANUAL_STUDENT_STATUSES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
                <button className="btn btn-outline-primary" onClick={() => apply(studentService.updateStatus(id, status))}>
                  Cambiar
                </button>
              </div>
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="career">
                Carrera
              </label>
              <div className="input-group">
                <select id="career" className="form-select" value={careerId} onChange={(e) => setCareerId(e.target.value)}>
                  {careers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                <button
                  className="btn btn-outline-primary"
                  onClick={() => apply(studentService.updateCareer(id, { careerId: Number(careerId) }))}
                >
                  Cambiar
                </button>
              </div>
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="plan">
                Plan de estudios
              </label>
              <div className="input-group">
                <select id="plan" className="form-select" value={planId} onChange={(e) => setPlanId(e.target.value)}>
                  {plans.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.code} {p.status === "CURRENT" ? "(vigente)" : ""}
                    </option>
                  ))}
                </select>
                <button
                  className="btn btn-outline-primary"
                  onClick={() => apply(studentService.updateStudyPlan(id, { studyPlanId: Number(planId) }))}
                >
                  Cambiar
                </button>
              </div>
            </div>
          </div>
          <small className="text-muted">
            Carrera y plan solo se pueden cambiar mientras el estudiante no tenga inscripciones ni historial.
          </small>
        </div>
      </div>

      <h4>Historial académico</h4>
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

export default StudentDetail;
