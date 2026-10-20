import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import gradeService from "../services/grade.service";
import sectionService from "../services/section.service";
import academicPeriodService from "../services/academicPeriod.service";
import studentService from "../services/student.service";
import useCurrentTeacher from "../hooks/useCurrentTeacher";
import ScheduleGrid from "../components/ScheduleGrid";
import StatusBadge from "../components/StatusBadge";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { loadCatalog } from "../utils/catalog";

const TeacherSectionGrades = () => {
  const { sectionId } = useParams();
  const { teacher, error: teacherError } = useCurrentTeacher();
  const [section, setSection] = useState(null);
  const [period, setPeriod] = useState(null);
  const [subject, setSubject] = useState(null);
  const [registrations, setRegistrations] = useState([]);
  const [students, setStudents] = useState({});
  const [grades, setGrades] = useState({});
  const [saved, setSaved] = useState("");
  const [error, setError] = useState("");

  const fail = (e) => setError(getErrorMessage(e));

  const loadRegistrations = () => {
    gradeService
      .getRegistrations(teacher.id, sectionId)
      .then((response) => {
        setRegistrations(response.data);
        const values = {};
        for (const r of response.data) values[r.id] = r.grade ?? "";
        setGrades(values);
      })
      .catch(fail);
  };

  useEffect(() => {
    if (!teacher) return;
    loadRegistrations();
    sectionService
      .get(sectionId)
      .then((response) => {
        setSection(response.data);
        academicPeriodService
          .get(response.data.academicPeriodId)
          .then((r) => setPeriod(r.data))
          .catch(fail);
        loadCatalog()
          .then((catalog) => setSubject(catalog.subjectsById[response.data.subjectId]))
          .catch(fail);
      })
      .catch(fail);
    // los nombres son un extra: si el rol no puede listar estudiantes se muestra solo el id
    studentService
      .getAll()
      .then((response) => {
        const byId = {};
        for (const s of response.data) byId[s.id] = s;
        setStudents(byId);
      })
      .catch(() => {});
  }, [teacher, sectionId]);

  const saveGrade = (registration) => {
    setError("");
    setSaved("");
    gradeService
      .registerGrade(teacher.id, sectionId, registration.id, Number(grades[registration.id]))
      .then(() => {
        setSaved(`Nota guardada (inscripción ${registration.id})`);
        loadRegistrations();
      })
      .catch(fail);
  };

  const isOpen = period && period.status === "OPEN";
  const blocks = [];
  if (section) {
    for (const b of section.scheduleBlocks) blocks.push({ ...b, label: subject?.code });
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>
          {subject ? `${subject.code} - ${subject.name}` : "Sección"} {period && <StatusBadge status={period.status} />}
        </h2>
        <Link className="btn btn-outline-secondary" to="/my-sections">
          Volver
        </Link>
      </div>
      <ErrorAlert message={teacherError || error} onClose={() => setError("")} />
      {saved && <div className="alert alert-success">{saved}</div>}
      {period && !isOpen && <div className="alert alert-secondary">El período está cerrado: las notas no se pueden modificar.</div>}

      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>RUN</th>
            <th>Estudiante</th>
            <th style={{ width: "160px" }}>Nota final</th>
            <th>Resultado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {registrations.map((r) => (
            <tr key={r.id}>
              <td>{students[r.studentId]?.run}</td>
              <td>{students[r.studentId]?.fullName ?? `Estudiante ${r.studentId}`}</td>
              <td>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  min="1"
                  max="7"
                  step="0.1"
                  disabled={!isOpen}
                  value={grades[r.id] ?? ""}
                  onChange={(e) => setGrades({ ...grades, [r.id]: e.target.value })}
                />
              </td>
              <td>{r.result && <StatusBadge status={r.result} />}</td>
              <td className="text-end">
                <button
                  className="btn btn-sm btn-primary"
                  disabled={!isOpen || grades[r.id] === ""}
                  onClick={() => saveGrade(r)}
                >
                  Guardar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h5 className="mt-4">Horario</h5>
      <ScheduleGrid blocks={blocks} />
    </div>
  );
};

export default TeacherSectionGrades;
