import useCurrentTeacher from "../../hooks/useCurrentTeacher";
import TeachingHistoryTable from "../../components/TeachingHistoryTable";
import ErrorAlert from "../../components/ErrorAlert";

const TeacherTeachingHistory = () => {
  const { teacher, error } = useCurrentTeacher();

  return (
    <div>
      <h2>Mi historial docente</h2>
      <ErrorAlert message={error} />
      {teacher && <TeachingHistoryTable teacherId={teacher.id} />}
    </div>
  );
};

export default TeacherTeachingHistory;
