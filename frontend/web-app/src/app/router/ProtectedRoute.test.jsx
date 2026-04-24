import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";

const mockUseAuth = vi.fn();

vi.mock("../../features/auth/AuthContext", () => ({
  useAuth: () => mockUseAuth()
}));

function renderWithRoutes() {
  return render(
    <MemoryRouter initialEntries={["/protected"]}>
      <Routes>
        <Route path="/login" element={<div>Login route</div>} />
        <Route path="/" element={<div>Home route</div>} />
        <Route
          path="/protected"
          element={
            <ProtectedRoute allowedRoles={["PATIENT"]}>
              <div>Protected content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  );
}

describe("ProtectedRoute", () => {
  it("shows loading state", () => {
    mockUseAuth.mockReturnValue({ loading: true, isAuthenticated: false, user: null });

    renderWithRoutes();

    expect(screen.getByText("Loading session...")).toBeInTheDocument();
  });

  it("redirects unauthenticated user to login", () => {
    mockUseAuth.mockReturnValue({ loading: false, isAuthenticated: false, user: null });

    renderWithRoutes();

    expect(screen.getByText("Login route")).toBeInTheDocument();
  });

  it("redirects user with invalid role to home", () => {
    mockUseAuth.mockReturnValue({
      loading: false,
      isAuthenticated: true,
      user: { role: "DOCTOR" }
    });

    renderWithRoutes();

    expect(screen.getByText("Home route")).toBeInTheDocument();
  });

  it("renders children for allowed role", () => {
    mockUseAuth.mockReturnValue({
      loading: false,
      isAuthenticated: true,
      user: { role: "PATIENT" }
    });

    renderWithRoutes();

    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
});
