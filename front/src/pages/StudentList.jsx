import { useEffect, useState } from "react";
import studentService from "../services/student.service";

const StudentList = () => {
  const [students, setStudents] = useState([]);

  useEffect(() => {
    studentService
      .getAll()
      .then((response) => setStudents(response.data))
      .catch((error) => console.log("Error al listar estudiantes", error));
  }, []);

  return (
    <div>
      <h2>Estudiantes</h2>
      <table className="table table-striped">
        <thead>
          <tr>
            <th>ID</th>
            <th>RUN</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          {students.map((s) => (
            <tr key={s.id}>
              <td>{s.id}</td>
              <td>{s.run}</td>
              <td>{s.fullName}</td>
              <td>{s.email}</td>
              <td>{s.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default StudentList;
