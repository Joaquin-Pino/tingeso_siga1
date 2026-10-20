import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import sectionService from "../services/section.service";
import teacherService from "../services/teacher.service";
import FormField from "../components/FormField";
import ScheduleGrid from "../components/ScheduleGrid";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { loadCatalog } from "../utils/catalog";

const SectionForm = () => {
  const { periodId, sectionId } = useParams();
  const navigate = useNavigate();
  const [catalog, setCatalog] = useState({ careers: [], plans: [], subjects: [], subjectsById: {} });
  const [teachers, setTeachers] = useState([]);
  const [planId, setPlanId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [teacherId, setTeacherId] = useState("");
  const [capacity, setCapacity] = useState(30);
  const [blocks, setBlocks] = useState([]);
  const [error, setError] = useState("");
  const back = `/periods/${periodId}`;

  const fail = (e) => setError(getErrorMessage(e));

  useEffect(() => {
    loadCatalog()
      .then((c) => {
        setCatalog(c);
        // por defecto, el plan vigente de la primera carrera
        for (const plan of c.plans) {
          if (plan.status === "CURRENT") {
            setPlanId((current) => current || String(plan.id));
          }
        }
      })
      .catch(fail);
    teacherService
      .getAll()
      .then((response) => {
        const active = [];
        for (const t of response.data) {
          if (t.status === "ACTIVE") active.push(t);
        }
        setTeachers(active);
      })
      .catch(fail);
    if (!sectionId) return;
    sectionService
      .get(sectionId)
      .then((response) => {
        const s = response.data;
        setSubjectId(String(s.subjectId));
        setTeacherId(String(s.teacherId));
        setCapacity(s.capacity);
        setBlocks(s.scheduleBlocks);
      })
      .catch(fail);
  }, [sectionId]);

  const toggleBlock = (day, module) => {
    const next = [];
    let found = false;
    for (const b of blocks) {
      if (b.day === day && b.module === module) found = true;
      else next.push(b);
    }
    if (!found) next.push({ day, module });
    setBlocks(next);
  };

  const subject = catalog.subjectsById[subjectId];
  const requiredBlocks = subject ? (subject.theoryHours + subject.exerciseHours + subject.labHours) / 2 : null;

  const planSubjects = [];
  for (const s of catalog.subjects) {
    if (String(s.studyPlanId) === planId) planSubjects.push(s);
  }

  const handleSubmit = (event) => {
    event.preventDefault();
    const scheduleBlocks = [];
    for (const b of blocks) scheduleBlocks.push({ day: b.day, module: b.module });
    const common = { teacherId: Number(teacherId), capacity: Number(capacity), scheduleBlocks };
    const request = sectionId
      ? sectionService.update(sectionId, common)
      : sectionService.create({ ...common, subjectId: Number(subjectId), academicPeriodId: Number(periodId) });
    request.then(() => navigate(back)).catch(fail);
  };

  return (
    <div className="col-md-9">
      <h2>{sectionId ? "Editar sección" : "Nueva sección"}</h2>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <form onSubmit={handleSubmit}>
        {sectionId ? (
          <p>
            Asignatura: <strong>{subject ? `${subject.code} - ${subject.name}` : subjectId}</strong>
          </p>
        ) : (
          <div className="row">
            <div className="col-md-6">
              <FormField label="Plan de estudios" name="plan" value={planId} onChange={(e) => { setPlanId(e.target.value); setSubjectId(""); }}>
                <option value="">Seleccione...</option>
                {catalog.plans.map((p) => (
                  <option key={p.id} value={p.id}>
                    {catalog.careers.find((c) => c.id === p.careerId)?.name} · {p.code}
                  </option>
                ))}
              </FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Asignatura" name="subject" value={subjectId} onChange={(e) => setSubjectId(e.target.value)} required>
                <option value="">Seleccione...</option>
                {planSubjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    S{s.semester} · {s.code} - {s.name}
                  </option>
                ))}
              </FormField>
            </div>
          </div>
        )}
        <div className="row">
          <div className="col-md-8">
            <FormField label="Docente responsable" name="teacher" value={teacherId} onChange={(e) => setTeacherId(e.target.value)} required>
              <option value="">Seleccione...</option>
              {teachers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}
                </option>
              ))}
            </FormField>
          </div>
          <div className="col-md-4">
            <FormField label="Capacidad (1 a 60)" name="capacity" type="number" min="1" max="60" value={capacity} onChange={(e) => setCapacity(e.target.value)} required />
          </div>
        </div>

        <label className="form-label">
          Bloques horarios (clic en la celda para marcar): {blocks.length}
          {requiredBlocks !== null && ` de ${requiredBlocks} requeridos (horas TEL / 2)`}
        </label>
        <ScheduleGrid blocks={blocks} onCellClick={toggleBlock} />

        <button type="submit" className="btn btn-primary me-2">
          Guardar
        </button>
        <button type="button" className="btn btn-outline-secondary" onClick={() => navigate(back)}>
          Cancelar
        </button>
      </form>
    </div>
  );
};

export default SectionForm;
