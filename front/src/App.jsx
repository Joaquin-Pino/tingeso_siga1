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
import PeriodList from "./pages/PeriodList";
import PeriodDetail from "./pages/PeriodDetail";
import PeriodEnrollments from "./pages/PeriodEnrollments";
import SectionForm from "./pages/SectionForm";
import StudentRegistrations from "./pages/StudentRegistrations";
import TeacherSections from "./pages/TeacherSections";
import TeacherSectionGrades from "./pages/TeacherSectionGrades";
import StudentProfile from "./pages/StudentProfile";
import StudentHistory from "./pages/StudentHistory";
import StudentOffering from "./pages/StudentOffering";
import StudentRegistration from "./pages/StudentRegistration";
import StudentMyRegistrations from "./pages/StudentMyRegistrations";
import NotFound from "./pages/NotFound";

const teacher = (element) => <PrivateRoute element={element} rolesAllowed={["TEACHER"]} />;
const student = (element) => <PrivateRoute element={element} rolesAllowed={["STUDENT"]} />;
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
          <Route path="/students/:id/registrations" element={admin(<StudentRegistrations />)} />

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

          <Route path="/periods" element={admin(<PeriodList />)} />
          <Route path="/periods/:id" element={admin(<PeriodDetail />)} />
          <Route path="/periods/:id/enrollments" element={admin(<PeriodEnrollments />)} />
          <Route path="/periods/:periodId/sections/new" element={admin(<SectionForm />)} />
          <Route path="/periods/:periodId/sections/:sectionId/edit" element={admin(<SectionForm />)} />

          <Route path="/my-sections" element={teacher(<TeacherSections />)} />
          <Route path="/my-sections/:sectionId" element={teacher(<TeacherSectionGrades />)} />

          <Route path="/my-profile" element={student(<StudentProfile />)} />
          <Route path="/my-history" element={student(<StudentHistory />)} />
          <Route path="/offering" element={student(<StudentOffering />)} />
          <Route path="/registration" element={student(<StudentRegistration />)} />
          <Route path="/my-registrations" element={student(<StudentMyRegistrations />)} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
