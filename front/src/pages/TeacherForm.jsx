import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import teacherService from "../services/teacher.service";
import FormField from "../components/FormField";
import ErrorAlert from "../components/ErrorAlert";
import { getErrorMessage } from "../utils/errors";
import { ACADEMIC_DEGREES } from "../utils/labels";

const EMPTY = {
  temporaryPassword: "",
  run: "",
  fullName: "",
  email: "",
  professionalTitle: "",
  academicDegree: "BACHELOR",
};

const TeacherForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    teacherService
      .get(id)
      .then((response) => setForm(response.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, [id]);

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const data = {
      fullName: form.fullName,
      email: form.email,
      professionalTitle: form.professionalTitle,
      academicDegree: form.academicDegree,
    };
    const request = id
      ? teacherService.update(id, data)
      : teacherService.create({ ...data, temporaryPassword: form.temporaryPassword, run: form.run });
    request.then(() => navigate("/teachers")).catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div className="col-md-6">
      <h2>{id ? "Editar docente" : "Nuevo docente"}</h2>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <form onSubmit={handleSubmit}>
        {!id && (
          <>
            <FormField label="Contraseña temporal" name="temporaryPassword" type="password" autoComplete="new-password" value={form.temporaryPassword} onChange={handleChange} required />
            <FormField label="RUN" name="run" value={form.run} onChange={handleChange} required />
          </>
        )}
        <FormField label="Nombre completo" name="fullName" value={form.fullName} onChange={handleChange} required />
        <FormField label="Correo" name="email" type="email" value={form.email} onChange={handleChange} required />
        <FormField label="Título profesional" name="professionalTitle" value={form.professionalTitle} onChange={handleChange} required />
        <FormField label="Grado académico" name="academicDegree" value={form.academicDegree} onChange={handleChange}>
          {ACADEMIC_DEGREES.map((d) => (
            <option key={d.value} value={d.value}>
              {d.label}
            </option>
          ))}
        </FormField>
        <button type="submit" className="btn btn-primary me-2">
          Guardar
        </button>
        <button type="button" className="btn btn-outline-secondary" onClick={() => navigate("/teachers")}>
          Cancelar
        </button>
      </form>
    </div>
  );
};

export default TeacherForm;
