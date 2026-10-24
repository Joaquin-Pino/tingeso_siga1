import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import academicPeriodService from "../../services/academicPeriod.service";
import teacherService from "../../services/teacher.service";
import sectionService from "../../services/section.service";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";
import { loadCatalog } from "../../utils/catalog";
import { formatBlocks } from "../../utils/schedule";

const PeriodDetail = () => {
  const { id } = useParams();
  const [period, setPeriod] = useState(null);
  const [sections, setSections] = useState([]);
  const [subjectsById, setSubjectsById] = useState({});
  const [teacherNames, setTeacherNames] = useState({});
  const [error, setError] = useState("");

  const fail = (e) => setError(getErrorMessage(e));

  const loadSections = () => {
    academicPeriodService
      .getSections(id)
      .then((response) => setSections(response.data))
      .catch(fail);
  };

  useEffect(() => {
    academicPeriodService
      .get(id)
      .then((response) => setPeriod(response.data))
      .catch(fail);
    loadSections();
    loadCatalog()
      .then((catalog) => setSubjectsById(catalog.subjectsById))
      .catch(fail);
    teacherService
      .getAll()
      .then((response) => {
        const names = {};
        for (const t of response.data) names[t.id] = t.fullName;
        setTeacherNames(names);
      })
      .catch(fail);
  }, [id]);

  const remove = (section) => {
    if (!window.confirm("¿Eliminar la sección?")) return;
    sectionService
      .remove(section.id)
      .then(loadSections)
      .catch(fail);
  };

  const isOpen = period && period.status === "OPEN";

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>
          Oferta {period ? period.code : ""} {period && <StatusBadge status={period.status} />}
        </h2>
        <div>
          <Link className="btn btn-outline-secondary me-2" to="/periods">
            Volver
          </Link>
          <Link className="btn btn-outline-primary me-2" to={`/periods/${id}/enrollments`}>
            Matrícula
          </Link>
          {isOpen && (
            <Link className="btn btn-primary" to={`/periods/${id}/sections/new`}>
              Nueva sección
            </Link>
          )}
        </div>
      </div>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Asignatura</th>
            <th>Docente</th>
            <th>Cupo</th>
            <th>Horario</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {sections.map((s) => {
            const subject = subjectsById[s.subjectId];
            return (
              <tr key={s.id}>
                <td>{subject ? `${subject.code} - ${subject.name}` : s.subjectId}</td>
                <td>{teacherNames[s.teacherId]}</td>
                <td>
                  {s.enrolledCount}/{s.capacity}
                </td>
                <td>{formatBlocks(s.scheduleBlocks)}</td>
                <td className="text-end">
                  {isOpen && (
                    <>
                      <Link className="btn btn-sm btn-outline-primary me-1" to={`/periods/${id}/sections/${s.id}/edit`}>
                        Editar
                      </Link>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => remove(s)}>
                        Eliminar
                      </button>
                    </>
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

export default PeriodDetail;
