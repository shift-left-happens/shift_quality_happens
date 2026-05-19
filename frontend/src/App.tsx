import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Layout } from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PlaceholderPage from './pages/PlaceholderPage';
import EmployeesListPage from './pages/employees/EmployeesListPage';
import EmployeeFormPage from './pages/employees/EmployeeFormPage';
import DepartmentsListPage from './pages/departments/DepartmentsListPage';
import DepartmentFormPage from './pages/departments/DepartmentFormPage';
import ShiftPlannerPage from './pages/shifts/ShiftPlannerPage';
import ShiftFormPage from './pages/shifts/ShiftFormPage';
import ShiftAssignmentsListPage from './pages/shift-assignments/ShiftAssignmentsListPage';
import ShiftAssignmentFormPage from './pages/shift-assignments/ShiftAssignmentFormPage';
import ShiftSwapsListPage from './pages/shift-swaps/ShiftSwapsListPage';
import ShiftSwapFormPage from './pages/shift-swaps/ShiftSwapFormPage';
import JobRolesListPage from './pages/job-roles/JobRolesListPage';
import JobRoleFormPage from './pages/job-roles/JobRoleFormPage';
import MySchedulePage from './pages/MySchedulePage';
import LeaveRequestsListPage from './pages/leave-requests/LeaveRequestsListPage';
import LeaveRequestFormPage from './pages/leave-requests/LeaveRequestFormPage';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<DashboardPage />} />
          <Route path="/settings" element={<PlaceholderPage title="Account settings" />} />
          <Route path="/my-schedule" element={<MySchedulePage />} />
          <Route path="/employees" element={<EmployeesListPage />} />
          <Route path="/employees/new" element={<EmployeeFormPage />} />
          <Route path="/employees/:id" element={<EmployeeFormPage />} />
          <Route path="/departments" element={<DepartmentsListPage />} />
          <Route path="/departments/new" element={<DepartmentFormPage />} />
          <Route path="/departments/:id" element={<DepartmentFormPage />} />
          <Route path="/shifts" element={<ShiftPlannerPage />} />
          <Route path="/shifts/new" element={<ShiftFormPage />} />
          <Route path="/shifts/:id" element={<ShiftFormPage />} />
          <Route path="/shift-assignments" element={<ShiftAssignmentsListPage />} />
          <Route path="/shift-assignments/new" element={<ShiftAssignmentFormPage />} />
          <Route path="/shift-assignments/:id" element={<ShiftAssignmentFormPage />} />
          <Route path="/shift-swaps" element={<ShiftSwapsListPage />} />
          <Route path="/shift-swaps/new" element={<ShiftSwapFormPage />} />
          <Route path="/job-roles" element={<JobRolesListPage />} />
          <Route path="/job-roles/new" element={<JobRoleFormPage />} />
          <Route path="/job-roles/:id" element={<JobRoleFormPage />} />
          <Route path="/leave-requests" element={<LeaveRequestsListPage />} />
          <Route path="/leave-requests/new" element={<LeaveRequestFormPage />} />
          <Route path="/leave-requests/:id" element={<LeaveRequestFormPage />} />
          <Route path="*" element={<PlaceholderPage title="Not found" />} />
        </Route>
      </Routes>
    </AuthProvider>
  );
}
