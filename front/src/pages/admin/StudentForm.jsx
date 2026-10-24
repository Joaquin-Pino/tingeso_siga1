import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import studentService from "../../services/student.service";
import careerService from "../../services/career.service";
import FormField from "../../components/FormField";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const EMPTY = { temporaryPassword: "", run: "", fullName: "", email: "", careerId: "" };

const StudentForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [careers, setCareers] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (id) {
      studentService
        .get(id)
        .then((response) => setForm(response.data))
        .catch((e) => setError(getErrorMessage(e)));
      return;
    }
    // una carrera inactiva no recibe estudiantes nuevos
    careerService
      .getAll()
      .then((response) => {
        const active = [];
        for (const c of response.data) {
          if (c.status === "ACTIVE") active.push(c);
        }
        setCareers(active);
        if (active.length > 0) setForm((f) => ({ ...f, careerId: active[0].id }));
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, [id]);

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const request = id
      ? studentService.update(id, { fullName: form.fullName, email: form.email })
      : studentService.create({
          temporaryPassword: form.temporaryPassword,
          run: form.run,
          fullName: form.fullName,
          email: form.email,
          careerId: Number(form.careerId),
        });
    request.then(() => navigate("/students")).catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div className="col-md-6">
      <h2>{id ? "Editar estudiante" : "Nuevo estudiante"}</h2>
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
        {!id && (
          <FormField label="Carrera" name="careerId" value={form.careerId} onChange={handleChange} required>
            {careers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </FormField>
        )}
        <button type="submit" className="btn btn-primary me-2">
          Guardar
        </button>
        <button type="button" className="btn btn-outline-secondary" onClick={() => navigate("/students")}>
          Cancelar
        </button>
      </form>
    </div>
  );
};

export default StudentForm;
