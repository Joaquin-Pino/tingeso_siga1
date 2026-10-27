import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import registrationService from "../../services/registration.service";
import useCurrentStudent from "../../hooks/useCurrentStudent";
import useOpenPeriod from "../../hooks/useOpenPeriod";
import usePlanOffering from "../../hooks/usePlanOffering";
import ScheduleGrid from "../../components/ScheduleGrid";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";
import { formatBlocks } from "../../utils/schedule";

const MAX_SECTIONS = 6;

const StudentRegistration = () => {
  const navigate = useNavigate();
  const { student, error: studentError } = useCurrentStudent();
  const { period, loaded, error: periodError } = useOpenPeriod();
  const { offering, error: offeringError } = usePlanOffering(period?.id, student?.studyPlanId);
  const [registeredSectionIds, setRegisteredSectionIds] = useState(new Set());
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!student || !period) return;
    registrationService
      .getAll(student.id, period.id)
      .then((response) => {
        const ids = new Set();
        for (const r of response.data) ids.add(r.sectionId);
        setRegisteredSectionIds(ids);
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, [student, period]);

  const toggle = (sectionId) => {
    const next = [];
    let found = false;
    for (const id of selected) {
      if (id === sectionId) found = true;
      else next.push(id);
    }
    if (!found) next.push(sectionId);
    setSelected(next);
  };

  const submit = () => {
    setError("");
    registrationService
      .register(student.id, selected)
      .then(() => navigate("/my-registrations"))
      .catch((e) => setError(getErrorMessage(e)));
  };

  // vista previa del horario con las secciones marcadas
  const previewBlocks = [];
  for (const s of offering) {
    if (!selected.includes(s.id)) continue;
    for (const b of s.scheduleBlocks) previewBlocks.push({ ...b, label: s.subject.code });
  }

  // el mínimo (3, o las pendientes si quedan menos) lo valida el back, que cuenta lo ya inscrito + lo nuevo
  const validCount = selected.length >= 1 && registeredSectionIds.size + selected.length <= MAX_SECTIONS;

  return (
    <div>
      <h2>Inscripción {period ? period.code : ""}</h2>
      <ErrorAlert message={studentError || periodError || offeringError || error} onClose={() => setError("")} />
      {loaded && !period && <div className="alert alert-info">No hay un período abierto.</div>}
      <p className="text-muted">
        Debes quedar con entre 3 y {MAX_SECTIONS} asignaturas inscritas en el período, contando las que ya tienes (si te quedan menos de 3
        para terminar el plan, las pendientes disponibles). Llevas {registeredSectionIds.size}. El sistema valida prerrequisitos, cupos, matrícula y choques de horario al confirmar.
      </p>

      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th></th>
            <th>Sem.</th>
            <th>Asignatura</th>
            <th>SCT</th>
            <th>Cupos</th>
            <th>Horario</th>
          </tr>
        </thead>
        <tbody>
          {offering.map((s) => {
            const registered = registeredSectionIds.has(s.id);
            const full = s.enrolledCount >= s.capacity;
            return (
              <tr key={s.id} className={registered ? "table-success" : ""}>
                <td>
                  <input
                    type="checkbox"
                    className="form-check-input"
                    disabled={registered || full}
                    checked={selected.includes(s.id)}
                    onChange={() => toggle(s.id)}
                  />
                </td>
                <td>{s.subject.semester}</td>
                <td>
                  {s.subject.code} - {s.subject.name} {registered && <span className="badge text-bg-success">Inscrito</span>}
                </td>
                <td>{s.subject.credits}</td>
                <td>{full ? <span className="badge text-bg-danger">Sin cupo</span> : `${s.capacity - s.enrolledCount} de ${s.capacity}`}</td>
                <td>{formatBlocks(s.scheduleBlocks)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <h5>Horario con la selección</h5>
      <ScheduleGrid blocks={previewBlocks} />

      <button className="btn btn-primary" disabled={!validCount} onClick={submit}>
        Inscribir {selected.length} asignatura{selected.length === 1 ? "" : "s"}
      </button>
    </div>
  );
};

export default StudentRegistration;
