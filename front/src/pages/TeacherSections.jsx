import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import teacherService from "../services/teacher.service";
import academicPeriodService from "../services/academicPeriod.service";
import useCurrentTeacher from "../hooks/useCurrentTeacher";
import StatusBadge from "../components/StatusBadge";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { loadCatalog } from "../utils/catalog";
import { formatBlocks } from "../utils/schedule";

const TeacherSections = () => {
  const { teacher, error: teacherError } = useCurrentTeacher();
  const [sections, setSections] = useState([]);
  const [periods, setPeriods] = useState({});
  const [subjectsById, setSubjectsById] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    if (!teacher) return;
    const fail = (e) => setError(getErrorMessage(e));
    teacherService
      .getSections(teacher.id)
      .then((response) => setSections(response.data))
      .catch(fail);
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
  }, [teacher]);

  return (
    <div>
      <h2>Mis secciones</h2>
      <ErrorAlert message={teacherError || error} onClose={() => setError("")} />
      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Período</th>
            <th>Asignatura</th>
            <th>Inscritos</th>
            <th>Horario</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {sections.map((s) => {
            const period = periods[s.academicPeriodId];
            const subject = subjectsById[s.subjectId];
            return (
              <tr key={s.id}>
                <td>
                  {period?.code} {period && <StatusBadge status={period.status} />}
                </td>
                <td>{subject ? `${subject.code} - ${subject.name}` : s.subjectId}</td>
                <td>
                  {s.enrolledCount}/{s.capacity}
                </td>
                <td>{formatBlocks(s.scheduleBlocks)}</td>
                <td className="text-end">
                  <Link className="btn btn-sm btn-primary" to={`/my-sections/${s.id}`}>
                    Inscritos y notas
                  </Link>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default TeacherSections;
