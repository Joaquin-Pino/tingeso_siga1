import { useEffect, useState } from "react";
import registrationService from "../services/registration.service";
import academicPeriodService from "../services/academicPeriod.service";
import useCurrentStudent from "../hooks/useCurrentStudent";
import StatusBadge from "../components/StatusBadge";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { loadCatalog } from "../utils/catalog";

const StudentMyRegistrations = () => {
  const { student, error: studentError } = useCurrentStudent();
  const [registrations, setRegistrations] = useState([]);
  const [periods, setPeriods] = useState({});
  const [subjectsById, setSubjectsById] = useState({});
  const [error, setError] = useState("");

  const fail = (e) => setError(getErrorMessage(e));

  const load = () => {
    registrationService
      .getAll(student.id)
      .then((response) => setRegistrations(response.data))
      .catch(fail);
  };

  useEffect(() => {
    if (!student) return;
    load();
    academicPeriodService
      .getAll()
      .then((response) => {
        const byId = {};
        for (const p of response.data) byId[p.id] = p;
        setPeriods(byId);
      })
      .catch(fail);
    loadCatalog()
      .then((catalog) => setSubjectsById(catalog.subjectsById))
      .catch(fail);
  }, [student]);

  const cancel = (registration) => {
    if (!window.confirm("¿Anular esta inscripción? Deben quedarte al menos 3 asignaturas en el período.")) return;
    setError("");
    registrationService
      .cancel(student.id, registration.id)
      .then(load)
      .catch(fail);
  };

  return (
    <div>
      <h2>Mis inscripciones</h2>
      <ErrorAlert message={studentError || error} onClose={() => setError("")} />
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Período</th>
            <th>Asignatura</th>
            <th>Fecha</th>
            <th>Nota</th>
            <th>Resultado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {registrations.map((r) => {
            const period = periods[r.academicPeriodId];
            const subject = subjectsById[r.subjectId];
            const canCancel = period && period.status === "OPEN" && r.grade === null;
            return (
              <tr key={r.id}>
                <td>{period?.code}</td>
                <td>{subject ? `${subject.code} - ${subject.name}` : r.subjectId}</td>
                <td>{r.registrationDate}</td>
                <td>{r.grade}</td>
                <td>{r.result && <StatusBadge status={r.result} />}</td>
                <td className="text-end">
                  {canCancel && (
                    <button className="btn btn-sm btn-outline-danger" onClick={() => cancel(r)}>
                      Anular
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default StudentMyRegistrations;
