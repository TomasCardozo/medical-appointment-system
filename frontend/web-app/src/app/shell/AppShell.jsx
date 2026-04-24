import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";

function navClassName({ isActive }) {
  const base = "rounded-lg px-3 py-2 text-sm font-semibold transition";
  if (isActive) {
    return `${base} bg-brand-600 text-white`;
  }
  return `${base} text-slate-700 hover:bg-brand-100`;
}

export function AppShell({ children }) {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className="min-h-screen px-4 pb-10 pt-6 sm:px-8">
      <header className="glass-panel mx-auto mb-6 flex w-full max-w-6xl flex-wrap items-center justify-between gap-4 rounded-2xl p-4 shadow-soft">
        <Link to="/" className="text-xl font-extrabold tracking-tight text-brand-700">
          TurnosMed
        </Link>

        <nav className="flex flex-wrap items-center gap-2">
          {!isAuthenticated && (
            <>
              <NavLink className={navClassName} to="/login">
                Login
              </NavLink>
              <NavLink className={navClassName} to="/register">
                Register
              </NavLink>
            </>
          )}

          {isAuthenticated && user?.role === "PATIENT" && (
            <>
              <NavLink className={navClassName} to="/patient">
                Dashboard
              </NavLink>
              <NavLink className={navClassName} to="/booking">
                Booking
              </NavLink>
            </>
          )}

          {isAuthenticated && user?.role === "DOCTOR" && (
            <NavLink className={navClassName} to="/doctor/agenda">
              Doctor Agenda
            </NavLink>
          )}

          {isAuthenticated && (
            <NavLink className={navClassName} to="/profile">
              Profile
            </NavLink>
          )}
        </nav>

        <div className="flex items-center gap-3">
          {isAuthenticated && (
            <>
              <div className="text-right text-sm text-slate-600">
                <p className="font-semibold text-slate-800">{user?.fullName}</p>
                <p>{user?.role}</p>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-lg border border-brand-600 px-3 py-2 text-sm font-semibold text-brand-700 transition hover:bg-brand-100"
              >
                Logout
              </button>
            </>
          )}
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl">{children}</main>
    </div>
  );
}
