import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import studyPlanService from "../services/studyPlan.service";
import subjectService from "../services/subject.service";
import FormField from "../components/FormField";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";

const EMPTY = {
  code: "",
  name: "",
  semester: 1,
  theoryHours: 0,
  exerciseHours: 0,
  labHours: 0,
  credits: 1,
  prerequisiteIds: [],
};

const SubjectForm = () => {
  const { careerId, planId, subjectId } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [planSubjects, setPlanSubjects] = useState([]);
  const [error, setError] = useState("");
  const back = `/careers/${careerId}/plans/${planId}`;

  useEffect(() => {
    studyPlanService
      .getSubjects(planId)
      .then((response) => setPlanSubjects(response.data))
      .catch((e) => setError(getErrorMessage(e)));
    if (!subjectId) return;
    subjectService
      .get(subjectId)
      .then((response) => setForm(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, [planId, subjectId]);

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value });
  };

  const togglePrerequisite = (id) => {
    const current = form.prerequisiteIds;
    const next = [];
    let found = false;
    for (const existing of current) {
      if (existing === id) found = true;
      else next.push(existing);
    }
    if (!found) next.push(id);
    setForm({ ...form, prerequisiteIds: next });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const data = {
      code: form.code,
      name: form.name,
      semester: Number(form.semester),
      theoryHours: Number(form.theoryHours),
      exerciseHours: Number(form.exerciseHours),
      labHours: Number(form.labHours),
      credits: Number(form.credits),
      prerequisiteIds: form.prerequisiteIds,
    };
    const request = subjectId
      ? subjectService.update(subjectId, data)
      : subjectService.create({ ...data, studyPlanId: Number(planId) });
    request.then(() => navigate(back)).catch((e) => setError(getErrorMessage(e)));
  };

  // solo asignaturas de semestres anteriores pueden ser prerrequisito
  const candidates = [];
  for (const s of planSubjects) {
    if (s.semester < Number(form.semester)) candidates.push(s);
  }

  return (
    <div className="col-md-7">
      <h2>{subjectId ? "Editar asignatura" : "Nueva asignatura"}</h2>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <form onSubmit={handleSubmit}>
        <div className="row">
          <div className="col-md-4">
            <FormField label="Código" name="code" value={form.code} onChange={handleChange} required />
          </div>
          <div className="col-md-8">
            <FormField label="Nombre" name="name" value={form.name} onChange={handleChange} required />
          </div>
        </div>
        <div className="row">
          <div className="col-md-3">
            <FormField label="Semestre" name="semester" value={form.semester} onChange={handleChange}>
              <option value={1}>1</option>
              <option value={2}>2</option>
              <option value={3}>3</option>
              <option value={4}>4</option>
            </FormField>
          </div>
          <div className="col-md-3">
            <FormField label="Teoría" name="theoryHours" type="number" min="0" value={form.theoryHours} onChange={handleChange} required />
          </div>
          <div className="col-md-3">
            <FormField label="Ejercicios" name="exerciseHours" type="number" min="0" value={form.exerciseHours} onChange={handleChange} required />
          </div>
          <div className="col-md-3">
            <FormField label="Laboratorio" name="labHours" type="number" min="0" value={form.labHours} onChange={handleChange} required />
          </div>
        </div>
        <FormField label="SCT (1 a 7)" name="credits" type="number" min="1" max="7" value={form.credits} onChange={handleChange} required />

        <label className="form-label">Prerrequisitos (máximo 3, de semestres anteriores)</label>
        <div className="mb-3">
          {candidates.length === 0 && <small className="text-muted">No hay asignaturas de semestres anteriores.</small>}
          {candidates.map((s) => (
            <div className="form-check" key={s.id}>
              <input
                id={`pre-${s.id}`}
                type="checkbox"
                className="form-check-input"
                checked={form.prerequisiteIds.includes(s.id)}
                onChange={() => togglePrerequisite(s.id)}
              />
              <label htmlFor={`pre-${s.id}`} className="form-check-label">
                {s.code} - {s.name} (sem. {s.semester})
              </label>
            </div>
          ))}
        </div>

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

export default SubjectForm;
