import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import PrivateRoute from "./components/PrivateRoute";
import Home from "./pages/Home";
import StudentList from "./pages/admin/StudentList";
import StudentForm from "./pages/admin/StudentForm";
import StudentDetail from "./pages/admin/StudentDetail";
import TeacherList from "./pages/admin/TeacherList";
import TeacherForm from "./pages/admin/TeacherForm";
import CareerList from "./pages/admin/CareerList";
import CareerForm from "./pages/admin/CareerForm";
import CareerDetail from "./pages/admin/CareerDetail";
import StudyPlanDetail from "./pages/admin/StudyPlanDetail";
import SubjectForm from "./pages/admin/SubjectForm";
import PeriodList from "./pages/admin/PeriodList";
import PeriodDetail from "./pages/admin/PeriodDetail";
import PeriodEnrollments from "./pages/admin/PeriodEnrollments";
import SectionForm from "./pages/admin/SectionForm";
import StudentRegistrations from "./pages/admin/StudentRegistrations";
import TeacherSections from "./pages/teacher/TeacherSections";
import TeacherSectionGrades from "./pages/teacher/TeacherSectionGrades";
import StudentProfile from "./pages/student/StudentProfile";
import StudentHistory from "./pages/student/StudentHistory";
import StudentOffering from "./pages/student/StudentOffering";
import StudentRegistration from "./pages/student/StudentRegistration";
import StudentMyRegistrations from "./pages/student/StudentMyRegistrations";
import NotFound from "./pages/NotFound";

import StudyPlanList from "./pages/admin/StudyPlanList";
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

          <Route path="/study-plans" element={admin(<StudyPlanList />)} />
          <Route path="/subjects/new" element={admin(<SubjectForm />)} />


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
