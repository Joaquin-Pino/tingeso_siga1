import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import teacherService from "../../services/teacher.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";
import { degreeLabel } from "../../utils/labels";

const TeacherList = () => {
  const [teachers, setTeachers] = useState([]);
  const [error, setError] = useState("");

  const load = () => {
    teacherService
      .getAll()
      .then((response) => setTeachers(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(load, []);

  const toggleStatus = (teacher) => {
    const next = teacher.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    teacherService
      .updateStatus(teacher.id, next)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  const remove = (teacher) => {
    if (!window.confirm(`¿Eliminar al docente ${teacher.fullName}?`)) return;
    teacherService
      .remove(teacher.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Docentes</h2>
        <Link className="btn btn-primary" to="/teachers/new">
          Nuevo docente
        </Link>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>RUN</th>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Título</th>
            <th>Grado</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {teachers.map((t) => (
            <tr key={t.id}>
              <td>{t.run}</td>
              <td>{t.fullName}</td>
              <td>{t.email}</td>
              <td>{t.professionalTitle}</td>
              <td>{degreeLabel(t.academicDegree)}</td>
              <td>
                <StatusBadge status={t.status} />
              </td>
              <td className="text-end">
                <Link className="btn btn-sm btn-outline-primary me-1" to={`/teachers/${t.id}/edit`}>
                  Editar
                </Link>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={() => toggleStatus(t)}>
                  {t.status === "ACTIVE" ? "Desactivar" : "Activar"}
                </button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => remove(t)}>
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

export default TeacherList;
