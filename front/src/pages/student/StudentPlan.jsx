import { useEffect, useState } from "react";
import studentService from "../../services/student.service";
import studyPlanService from "../../services/studyPlan.service";
import registrationService from "../../services/registration.service";
import useCurrentStudent from "../../hooks/useCurrentStudent";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const SEMESTERS = [1, 2, 3, 4];

const STATE_BADGES = {
  PASSED: { label: "Aprobada", color: "success" },
  IN_PROGRESS: { label: "Cursando", color: "primary" },
  FAILED: { label: "Reprobada", color: "danger" },
  PENDING: { label: "Pendiente", color: "secondary" },
};

const StudentPlan = () => {
  const { student, error: studentError } = useCurrentStudent();
  const [subjects, setSubjects] = useState([]);
  const [history, setHistory] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!student) return;
    const fail = (e) => setError(getErrorMessage(e));
    studyPlanService
      .getSubjects(student.studyPlanId)
      .then((response) => setSubjects(response.data))
      .catch(fail);
    studentService
      .getAcademicHistory(student.id)
      .then((response) => setHistory(response.data))
      .catch(fail);
    registrationService
      .getAll(student.id)
      .then((response) => setRegistrations(response.data))
      .catch(fail);
  }, [student]);

  const stateOf = (subjectId) => {
    let failed = false;
    for (const r of history) {
      if (r.subjectId !== subjectId) continue;
      if (r.result === "PASSED") return "PASSED";
      failed = true;
    }
    // inscrita en un período que todavía no se cierra (sin resultado)
    for (const r of registrations) {
      if (r.subjectId === subjectId && !r.result) return "IN_PROGRESS";
    }
    return failed ? "FAILED" : "PENDING";
  };

  const codeOf = {};
  for (const s of subjects) codeOf[s.id] = s.code;

  let passedCredits = 0;
  let totalCredits = 0;
  for (const s of subjects) {
    totalCredits += s.credits;
    if (stateOf(s.id) === "PASSED") passedCredits += s.credits;
  }

  return (
    <div>
      <h2>Mi plan de estudios</h2>
      <ErrorAlert message={studentError || error} onClose={() => setError("")} />
      {subjects.length > 0 && (
        <p>
          Avance: <strong>{passedCredits}</strong> de {totalCredits} SCT aprobados
        </p>
      )}
      {SEMESTERS.map((semester) => {
        const rows = subjects.filter((s) => s.semester === semester);
        if (rows.length === 0) return null;
        return (
          <div key={semester} className="mb-4">
            <h5>Semestre {semester}</h5>
            <table className="table table-striped align-middle">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Asignatura</th>
                  <th>SCT</th>
                  <th>Horas (T-E-L)</th>
                  <th>Prerrequisitos</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((s) => {
                  const badge = STATE_BADGES[stateOf(s.id)];
                  const prerequisites = [];
                  for (const id of s.prerequisiteIds) prerequisites.push(codeOf[id]);
                  return (
                    <tr key={s.id}>
                      <td>{s.code}</td>
                      <td>{s.name}</td>
                      <td>{s.credits}</td>
                      <td>
                        {s.theoryHours}-{s.exerciseHours}-{s.labHours}
                      </td>
                      <td>{prerequisites.length > 0 ? prerequisites.join(", ") : "—"}</td>
                      <td>
                        <span className={`badge text-bg-${badge.color}`}>{badge.label}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        );
      })}
    </div>
  );
};

export default StudentPlan;
