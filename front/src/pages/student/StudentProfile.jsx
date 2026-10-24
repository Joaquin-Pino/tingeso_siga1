import { useEffect, useState } from "react";
import studentService from "../../services/student.service";
import careerService from "../../services/career.service";
import useCurrentStudent from "../../hooks/useCurrentStudent";
import StatusBadge from "../../components/StatusBadge";
import ErrorAlert from "../../components/ErrorAlert";
import { getErrorMessage } from "../../utils/errors";

const StudentProfile = () => {
  const { student, error: studentError } = useCurrentStudent();
  const [career, setCareer] = useState(null);
  const [plan, setPlan] = useState(null);
  const [average, setAverage] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!student) return;
    const fail = (e) => setError(getErrorMessage(e));
    careerService
      .get(student.careerId)
      .then((response) => setCareer(response.data))
      .catch(fail);
    careerService
      .getStudyPlans(student.careerId)
      .then((response) => {
        for (const p of response.data) {
          if (p.id === student.studyPlanId) setPlan(p);
        }
      })
      .catch(fail);
    studentService
      .getAcademicAverage(student.id)
      .then((response) => setAverage(response.data))
      .catch(fail);
  }, [student]);

  return (
    <div>
      <h2>Mi perfil</h2>
      <ErrorAlert message={studentError || error} onClose={() => setError("")} />
      {student && (
        <div className="card col-md-7">
          <div className="card-body">
            <h4 className="card-title">
              {student.fullName} <StatusBadge status={student.status} />
            </h4>
            <dl className="row mb-0">
              <dt className="col-sm-4">RUN</dt>
              <dd className="col-sm-8">{student.run}</dd>
              <dt className="col-sm-4">Correo</dt>
              <dd className="col-sm-8">{student.email}</dd>
              <dt className="col-sm-4">Carrera</dt>
              <dd className="col-sm-8">{career?.name}</dd>
              <dt className="col-sm-4">Plan de estudios</dt>
              <dd className="col-sm-8">{plan?.code}</dd>
              <dt className="col-sm-4">Promedio ponderado</dt>
              <dd className="col-sm-8">
                {average ? `${average.average ?? "sin historial"} (${average.subjectsCount} asignaturas, ${average.totalCredits} SCT)` : ""}
              </dd>
            </dl>
          </div>
        </div>
      )}
    </div>
  );
};

export default StudentProfile;
