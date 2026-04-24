import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./router/ProtectedRoute";
import { AppShell } from "./shell/AppShell";
import { useAuth } from "../features/auth/AuthContext";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { RegisterPage } from "../features/auth/pages/RegisterPage";
import { BookingPage } from "../features/booking/pages/BookingPage";
import { DoctorAgendaPage } from "../features/doctor/pages/DoctorAgendaPage";
import { PatientDashboardPage } from "../features/patient/pages/PatientDashboardPage";
import { ProfilePage } from "../features/profile/pages/ProfilePage";

function HomeRedirect() {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role === "DOCTOR") {
    return <Navigate to="/doctor/agenda" replace />;
  }

  return <Navigate to="/patient" replace />;
}

export function AppRoutes() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          path="/patient"
          element={
            <ProtectedRoute allowedRoles={["PATIENT"]}>
              <PatientDashboardPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/booking"
          element={
            <ProtectedRoute allowedRoles={["PATIENT"]}>
              <BookingPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/doctor/agenda"
          element={
            <ProtectedRoute allowedRoles={["DOCTOR"]}>
              <DoctorAgendaPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}
