import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import studentService from "../../services/student.service";
import careerService from "../../services/career.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const StudentList = () => {
  const [students, setStudents] = useState([]);
  const [careerNames, setCareerNames] = useState({});
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");

  const load = () => {
    studentService
      .getAll()
      .then((response) => setStudents(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  };

  useEffect(() => {
    load();
    careerService
      .getAll()
      .then((response) => {
        const names = {};
        for (const c of response.data) names[c.id] = c.name;
        setCareerNames(names);
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, []);

  const remove = (student) => {
    if (!window.confirm(`¿Eliminar al estudiante ${student.fullName}?`)) return;
    studentService
      .remove(student.id)
      .then(load)
      .catch((e) => setError(getErrorMessage(e)));
  };

  const text = search.toLowerCase();
  const visible = [];
  for (const s of students) {
    const matches =
      s.fullName.toLowerCase().includes(text) || s.run.toLowerCase().includes(text) || s.email.toLowerCase().includes(text);
    if (matches) visible.push(s);
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Estudiantes</h2>
        <Link className="btn btn-primary" to="/students/new">
          Nuevo estudiante
        </Link>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <input
        className="form-control mb-3"
        placeholder="Buscar por nombre, RUN o correo"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>RUN</th>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Carrera</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {visible.map((s) => (
            <tr key={s.id}>
              <td>{s.run}</td>
              <td>
                <Link to={`/students/${s.id}`}>{s.fullName}</Link>
              </td>
              <td>{s.email}</td>
              <td>{careerNames[s.careerId]}</td>
              <td>
                <StatusBadge status={s.status} />
              </td>
              <td className="text-end">
                <Link className="btn btn-sm btn-outline-primary me-1" to={`/students/${s.id}/edit`}>
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
  );
};

export default StudentList;
