import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import careerService from "../../services/career.service";
import studyPlanService from "../../services/studyPlan.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const CareerDetail = () => {
  const { id } = useParams();
  const [career, setCareer] = useState(null);
  const [plans, setPlans] = useState([]);
  const [newCode, setNewCode] = useState("");
  const [editing, setEditing] = useState(null);
  const [editCode, setEditCode] = useState("");
  const [error, setError] = useState("");

  const load = () => {
    careerService
      .get(id)
      .then((response) => setCareer(response.data))
      .catch((e) => setError(getErrorMessage(e)));
    careerService
      .getStudyPlans(id)
      .then((response) => setPlans(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(load, [id]);

  const createPlan = (event) => {
    event.preventDefault();
    studyPlanService
      .create({ careerId: Number(id), code: newCode })
      .then(() => {
        setNewCode("");
        load();
      })
      .catch((e) => setError(getErrorMessage(e)));
  };

  const savePlan = (plan) => {
    studyPlanService
      .update(plan.id, { code: editCode })
      .then(() => {
        setEditing(null);
        load();
      })
      .catch((e) => setError(getErrorMessage(e)));
  };

  const removePlan = (plan) => {
    if (!window.confirm(`¿Eliminar el plan ${plan.code}?`)) return;
    studyPlanService
      .remove(plan.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  if (!career) return <ErrorAlert message={error} />;

  return (
    <div>
      <h2>
        {career.name} <StatusBadge status={career.status} />
      </h2>
      <p className="text-muted">
        Código {career.code} · {career.description}
      </p>
      <ErrorAlert message={error} onClose={() => setError("")} />

      <h4 className="mt-4">Planes de estudio</h4>
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Plan</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {plans.map((p) => (
            <tr key={p.id}>
              <td>
                {editing === p.id ? (
                  <input className="form-control form-control-sm" value={editCode} onChange={(e) => setEditCode(e.target.value)} />
                ) : (
                  <Link to={`/careers/${id}/plans/${p.id}`}>{p.code}</Link>
                )}
              </td>
              <td>
                <StatusBadge status={p.status} />
              </td>
              <td className="text-end">
                {editing === p.id ? (
                  <>
                    <button className="btn btn-sm btn-primary me-1" onClick={() => savePlan(p)}>
                      Guardar
                    </button>
                    <button className="btn btn-sm btn-outline-secondary" onClick={() => setEditing(null)}>
                      Cancelar
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      className="btn btn-sm btn-outline-primary me-1"
                      onClick={() => {
                        setEditing(p.id);
                        setEditCode(p.code);
                      }}
                    >
                      Editar
                    </button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => removePlan(p)}>
                      Eliminar
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <form className="row g-2 align-items-end" onSubmit={createPlan}>
        <div className="col-auto">
          <label className="form-label" htmlFor="newCode">
            Nuevo plan vigente (ej. 2027.1)
          </label>
          <input id="newCode" className="form-control" value={newCode} onChange={(e) => setNewCode(e.target.value)} required />
        </div>
        <div className="col-auto">
          <button className="btn btn-primary" type="submit">
            Crear plan
          </button>
        </div>
      </form>
    </div>
  );
};

export default CareerDetail;
