import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import careerService from "../../services/career.service";
import studyPlanService from "../../services/studyPlan.service";
import subjectService from "../../services/subject.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const SEMESTERS = [1, 2, 3, 4];

const StudyPlanDetail = () => {
  const { careerId, planId } = useParams();
  const [plan, setPlan] = useState(null);
  const [subjects, setSubjects] = useState([]);
  const [error, setError] = useState("");

  const load = () => {
    // el back no expone GET de un plan: se busca entre los de la carrera
    careerService
      .getStudyPlans(careerId)
      .then((response) => {
        for (const p of response.data) {
          if (String(p.id) === planId) setPlan(p);
        }
      })
      .catch((e) => setError(getErrorMessage(e)));
    studyPlanService
      .getSubjects(planId)
      .then((response) => setSubjects(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(load, [careerId, planId]);

  const remove = (subject) => {
    if (!window.confirm(`¿Eliminar la asignatura ${subject.name}?`)) return;
    subjectService
      .remove(subject.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  const codeOf = (subjectId) => {
    for (const s of subjects) {
      if (s.id === subjectId) return s.code;
    }
    return subjectId;
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>
          Plan {plan ? plan.code : ""} {plan && <StatusBadge status={plan.status} />}
        </h2>
        <div>
          <Link className="btn btn-outline-secondary me-2" to={`/careers/${careerId}`}>
            Volver a la carrera
          </Link>
          <Link className="btn btn-primary" to={`/careers/${careerId}/plans/${planId}/subjects/new`}>
            Nueva asignatura
          </Link>
        </div>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />

      {SEMESTERS.map((semester) => (
        <div key={semester} className="mb-4">
          <h5>Semestre {semester}</h5>
          <table className="table table-sm table-striped align-middle">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>TEL</th>
                <th>SCT</th>
                <th>Prerrequisitos</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {subjects
                .filter((s) => s.semester === semester)
                .map((s) => (
                  <tr key={s.id}>
                    <td>{s.code}</td>
                    <td>{s.name}</td>
                    <td>
                      {s.theoryHours}-{s.exerciseHours}-{s.labHours}
                    </td>
                    <td>{s.credits}</td>
                    <td>{s.prerequisiteIds.map(codeOf).join(", ")}</td>
                    <td className="text-end">
                      <Link
                        className="btn btn-sm btn-outline-primary me-1"
                        to={`/careers/${careerId}/plans/${planId}/subjects/${s.id}/edit`}
                      >
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => remove(s)}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
};

export default StudyPlanDetail;
