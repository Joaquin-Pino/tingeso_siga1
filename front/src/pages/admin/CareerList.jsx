import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import careerService from "../../services/career.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const CareerList = () => {
  const [careers, setCareers] = useState([]);
  const [error, setError] = useState("");

  const load = () => {
    careerService
      .getAll()
      .then((response) => setCareers(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(load, []);

  const toggleStatus = (career) => {
    const next = career.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    careerService
      .updateStatus(career.id, next)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  const remove = (career) => {
    if (!window.confirm(`¿Eliminar la carrera ${career.name}?`)) return;
    careerService
      .remove(career.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Carreras</h2>
        <Link className="btn btn-primary" to="/careers/new">
          Nueva carrera
        </Link>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Código</th>
            <th>Nombre</th>
            <th>Año inicio</th>
            <th>Vacantes</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {careers.map((c) => (
            <tr key={c.id}>
              <td>{c.code}</td>
              <td>
                <Link to={`/careers/${c.id}`}>{c.name}</Link>
              </td>
              <td>{c.startYear}</td>
              <td>{c.vacancies}</td>
              <td>
                <StatusBadge status={c.status} />
              </td>
              <td className="text-end">
                <Link className="btn btn-sm btn-outline-primary me-1" to={`/careers/${c.id}/edit`}>
                  Editar
                </Link>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={() => toggleStatus(c)}>
                  {c.status === "ACTIVE" ? "Desactivar" : "Activar"}
                </button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => remove(c)}>
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

export default CareerList;
