import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import careerService from "../../services/career.service";
import studyPlanService from "../../services/studyPlan.service";
import FormField from "../../components/FormField";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const StudyPlanList = () => {
  const navigate = useNavigate();
  const [careers, setCareers] = useState([]);
  // un plan por fila, con el nombre de su carrera: el back solo lista los planes por carrera
  const [rows, setRows] = useState([]);
  const [careerId, setCareerId] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState("");

  const load = () => {
    careerService
      .getAll()
      .then((response) => {
        const loaded = response.data;
        setCareers(loaded);
        if (careerId === "" && loaded.length > 0) setCareerId(String(loaded[0].id));
        const requests = [];
        for (const career of loaded) {
          requests.push(careerService.getStudyPlans(career.id));
        }
        return Promise.all(requests).then((responses) => {
          const all = [];
          for (let i = 0; i < loaded.length; i++) {
            for (const plan of responses[i].data) {
              all.push({ ...plan, careerName: loaded[i].name });
            }
          }
          setRows(all);
        });
      })
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(load, []);

  const createPlan = (event) => {
    event.preventDefault();
    studyPlanService
      .create({ careerId: Number(careerId), code })
      .then((response) => navigate(`/careers/${careerId}/plans/${response.data.id}`))
      .catch((e) => setError(getErrorMessage(e)));
  };

  const removePlan = (plan) => {
    if (!window.confirm(`¿Eliminar el plan ${plan.code} de ${plan.careerName}?`)) return;
    studyPlanService
      .remove(plan.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div>
      <h2 className="mb-3">Planes de estudio</h2>
      <ErrorAlert message={error} onClose={() => setError("")} />

      <form className="row g-2 align-items-end mb-4" onSubmit={createPlan}>
        <div className="col-md-4">
          <FormField label="Carrera" name="careerId" value={careerId} onChange={(e) => setCareerId(e.target.value)} required>
            {careers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </FormField>
        </div>
        <div className="col-md-3">
          <FormField label="Código del plan (ej. 2027.1)" name="code" value={code} onChange={(e) => setCode(e.target.value)} required />
        </div>
        <div className="col-auto mb-3">
          <button className="btn btn-primary" type="submit" disabled={careers.length === 0}>
            Crear plan
          </button>
        </div>
        <small className="text-muted">El plan nuevo queda vigente y el vigente anterior de la carrera pasa a no vigente. Al crearlo se abre para agregar asignaturas.</small>
      </form>

      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Carrera</th>
            <th>Plan</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id}>
              <td>
                <Link to={`/careers/${p.careerId}`}>{p.careerName}</Link>
              </td>
              <td>
                <Link to={`/careers/${p.careerId}/plans/${p.id}`}>{p.code}</Link>
              </td>
              <td>
                <StatusBadge status={p.status} />
              </td>
              <td className="text-end">
                <Link className="btn btn-sm btn-outline-primary me-1" to={`/careers/${p.careerId}/plans/${p.id}`}>
                  Ver asignaturas
                </Link>
                <button className="btn btn-sm btn-outline-danger" onClick={() => removePlan(p)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default StudyPlanList;
