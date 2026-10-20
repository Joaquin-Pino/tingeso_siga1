import useCurrentStudent from "../hooks/useCurrentStudent";
import useOpenPeriod from "../hooks/useOpenPeriod";
import usePlanOffering from "../hooks/usePlanOffering";
import ErrorAlert from "../components/ErrorAlert";
import { formatBlocks } from "../utils/schedule";

const StudentOffering = () => {
  const { student, error: studentError } = useCurrentStudent();
  const { period, loaded, error: periodError } = useOpenPeriod();
  const { offering, error: offeringError } = usePlanOffering(period?.id, student?.studyPlanId);

  return (
    <div>
      <h2>Oferta académica {period ? period.code : ""}</h2>
      <ErrorAlert message={studentError || periodError || offeringError} />
      {loaded && !period && <div className="alert alert-info">No hay un período abierto.</div>}

      <table className="table table-striped align-middle">
        <thead>
          <tr>
            <th>Sem.</th>
            <th>Asignatura</th>
            <th>SCT</th>
            <th>Cupos</th>
            <th>Horario</th>
          </tr>
        </thead>
        <tbody>
          {offering.map((s) => (
            <tr key={s.id}>
              <td>{s.subject.semester}</td>
              <td>
                {s.subject.code} - {s.subject.name}
              </td>
              <td>{s.subject.credits}</td>
              <td>
                {s.capacity - s.enrolledCount} de {s.capacity}
              </td>
              <td>{formatBlocks(s.scheduleBlocks)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default StudentOffering;
