import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import PrivateRoute from "./components/PrivateRoute";
import Home from "./pages/Home";
import StudentList from "./pages/StudentList";
import StudentForm from "./pages/StudentForm";
import StudentDetail from "./pages/StudentDetail";
import TeacherList from "./pages/TeacherList";
import TeacherForm from "./pages/TeacherForm";
import CareerList from "./pages/CareerList";
import CareerForm from "./pages/CareerForm";
import CareerDetail from "./pages/CareerDetail";
import StudyPlanDetail from "./pages/StudyPlanDetail";
import SubjectForm from "./pages/SubjectForm";
import NotFound from "./pages/NotFound";

const admin = (element) => <PrivateRoute element={element} rolesAllowed={["ADMIN"]} />;

function App() {
  return (
    <Router>
      <Navbar />
      <div className="container pb-5">
        <Routes>
          <Route path="/" element={<Navigate to="/home" replace />} />
          <Route path="/home" element={<Home />} />

          <Route path="/students" element={admin(<StudentList />)} />
          <Route path="/students/new" element={admin(<StudentForm />)} />
          <Route path="/students/:id" element={admin(<StudentDetail />)} />
          <Route path="/students/:id/edit" element={admin(<StudentForm />)} />

          <Route path="/teachers" element={admin(<TeacherList />)} />
          <Route path="/teachers/new" element={admin(<TeacherForm />)} />
          <Route path="/teachers/:id/edit" element={admin(<TeacherForm />)} />

          <Route path="/careers" element={admin(<CareerList />)} />
          <Route path="/careers/new" element={admin(<CareerForm />)} />
          <Route path="/careers/:id" element={admin(<CareerDetail />)} />
          <Route path="/careers/:id/edit" element={admin(<CareerForm />)} />
          <Route path="/careers/:careerId/plans/:planId" element={admin(<StudyPlanDetail />)} />
          <Route path="/careers/:careerId/plans/:planId/subjects/new" element={admin(<SubjectForm />)} />
          <Route path="/careers/:careerId/plans/:planId/subjects/:subjectId/edit" element={admin(<SubjectForm />)} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
