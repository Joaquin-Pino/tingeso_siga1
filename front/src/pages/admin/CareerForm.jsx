import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import careerService from "../../services/career.service";
import FormField from "../../components/FormField";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const EMPTY = { code: "", name: "", description: "", startYear: "", vacancies: "" };

const CareerForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    careerService
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
      name: form.name,
      description: form.description,
      startYear: Number(form.startYear),
      vacancies: Number(form.vacancies),
    };
    
    const request = id
      ? careerService.update(id, data)
      : careerService.create({ ...data, code: form.code });
    request.then(() => navigate("/careers")).catch((e) => setError(getErrorMessage(e)));
  };

  return (
    <div className="col-md-6">
      <h2>{id ? "Editar carrera" : "Nueva carrera"}</h2>
      <ErrorAlert message={error} onClose={() => setError("")} />
      <form onSubmit={handleSubmit}>
        <FormField label="Código" name="code" value={form.code} onChange={handleChange} disabled={!!id} required />
        <FormField label="Nombre" name="name" value={form.name} onChange={handleChange} required />
        <FormField label="Descripción" name="description" value={form.description} onChange={handleChange} required />
        <FormField label="Año de inicio" name="startYear" type="number" value={form.startYear} onChange={handleChange} required />
        <FormField label="Vacantes" name="vacancies" type="number" min="0" value={form.vacancies} onChange={handleChange} required />
        <button type="submit" className="btn btn-primary me-2">
          Guardar
        </button>
        <button type="button" className="btn btn-outline-secondary" onClick={() => navigate("/careers")}>
          Cancelar
        </button>
      </form>
    </div>
  );
};

export default CareerForm;
