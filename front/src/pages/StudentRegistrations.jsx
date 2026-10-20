import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import registrationService from "../services/registration.service";
import studentService from "../services/student.service";
import academicPeriodService from "../services/academicPeriod.service";
import StatusBadge from "../components/StatusBadge";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { loadCatalog } from "../utils/catalog";

const StudentRegistrations = () => {
  const { id } = useParams();
  const [student, setStudent] = useState(null);
  const [registrations, setRegistrations] = useState([]);
  const [subjectsById, setSubjectsById] = useState({});
  const [periodCodes, setPeriodCodes] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    const fail = (e) => setError(getErrorMessage(e));
    studentService
      .get(id)
      .then((response) => setStudent(response.data))
      .catch(fail);
    registrationService
      .getAll(id)
      .then((response) => setRegistrations(response.data))
      .catch(fail);
    loadCatalog()
      .then((catalog) => setSubjectsById(catalog.subjectsById))
      .catch(fail);
    academicPeriodService
      .getAll()
      .then((response) => {
        const codes = {};
        for (const p of response.data) codes[p.id] = p.code;
        setPeriodCodes(codes);
      })
      .catch(fail);
  }, [id]);

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Inscripciones{student ? ` de ${student.fullName}` : ""}</h2>
        <Link className="btn btn-outline-secondary" to={`/students/${id}`}>
          Volver
        </Link>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <table className="table table-striped">
        <thead>
          <tr>
            <th>Período</th>
            <th>Asignatura</th>
            <th>Sección</th>
            <th>Fecha</th>
            <th>Nota</th>
            <th>Resultado</th>
          </tr>
        </thead>
        <tbody>
          {registrations.map((r) => (
            <tr key={r.id}>
              <td>{periodCodes[r.academicPeriodId]}</td>
              <td>{subjectsById[r.subjectId] ? `${subjectsById[r.subjectId].code} - ${subjectsById[r.subjectId].name}` : r.subjectId}</td>
              <td>{r.sectionId}</td>
              <td>{r.registrationDate}</td>
              <td>{r.grade}</td>
              <td>{r.result && <StatusBadge status={r.result} />}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default StudentRegistrations;
