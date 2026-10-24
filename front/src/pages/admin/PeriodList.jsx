import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import academicPeriodService from "../../services/academicPeriod.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const PeriodList = () => {
  const [periods, setPeriods] = useState([]);
  const [year, setYear] = useState(new Date().getFullYear());
  const [semester, setSemester] = useState("FIRST");
  const [closeResult, setCloseResult] = useState(null);
  const [error, setError] = useState("");

  const load = () => {
    academicPeriodService
      .getAll()
      .then((response) => setPeriods(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(load, []);

  const create = (event) => {
    event.preventDefault();
    setError("");
    academicPeriodService
      .create({ year: Number(year), semester })
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  const close = (period) => {
    if (!window.confirm(`¿Cerrar el período ${period.code}? Esta acción consolida el historial y no se puede deshacer.`)) return;
    setError("");
    setCloseResult(null);
    academicPeriodService
      .close(period.id)
      .then((response) => {
        setCloseResult(response.data);
        load();
      })
      .catch((e) => setError(getErrorMessage(e)));
  };

  const remove = (period) => {
    if (!window.confirm(`¿Eliminar el período ${period.code}?`)) return;
    academicPeriodService
      .remove(period.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div>
      <h2>Períodos académicos</h2>
      <ErrorAlert message={error} onClose={() => setError("")} />
      {closeResult && (
        <div className="alert alert-success" role="alert">
          Período {closeResult.period.code} cerrado: {closeResult.recordsCreated} registros en el historial,{" "}
          {closeResult.graduatedStudentIds.length} egresados, {closeResult.eliminatedStudentIds.length} eliminados.
        </div>
      )}

      <form className="row g-2 align-items-end mb-4" onSubmit={create}>
        <div className="col-auto">
          <label className="form-label" htmlFor="year">
            Año
          </label>
          <input id="year" type="number" className="form-control" value={year} onChange={(e) => setYear(e.target.value)} required />
        </div>
        <div className="col-auto">
          <label className="form-label" htmlFor="semester">
            Semestre
          </label>
          <select id="semester" className="form-select" value={semester} onChange={(e) => setSemester(e.target.value)}>
            <option value="FIRST">1</option>
            <option value="SECOND">2</option>
          </select>
        </div>
        <div className="col-auto">
          <button className="btn btn-primary" type="submit">
            Crear período
          </button>
        </div>
      </form>

      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Código</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.id}>
              <td>
                <Link to={`/periods/${p.id}`}>{p.code}</Link>
              </td>
              <td>
                <StatusBadge status={p.status} />
              </td>
              <td className="text-end">
                <Link className="btn btn-sm btn-outline-primary me-1" to={`/periods/${p.id}`}>
                  Oferta
                </Link>
                <Link className="btn btn-sm btn-outline-primary me-1" to={`/periods/${p.id}/enrollments`}>
                  Matrícula
                </Link>
                {p.status === "OPEN" && (
                  <button className="btn btn-sm btn-warning me-1" onClick={() => close(p)}>
                    Cerrar
                  </button>
                )}
                <button className="btn btn-sm btn-outline-danger" onClick={() => remove(p)}>
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

export default PeriodList;
