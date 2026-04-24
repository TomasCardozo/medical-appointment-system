import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { LoginPage } from "./LoginPage";

const mockLogin = vi.fn();

vi.mock("../AuthContext", () => ({
  useAuth: () => ({
    login: mockLogin
  })
}));

function renderPage(entry = "/login") {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/patient" element={<div>Patient Route</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("LoginPage", () => {
  beforeEach(() => {
    mockLogin.mockReset();
  });

  it("submits credentials and navigates on success", async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue(undefined);

    renderPage({ pathname: "/login", state: { from: { pathname: "/patient" } } });

    await user.type(screen.getByLabelText("Email"), "patient@example.com");
    await user.type(screen.getByLabelText("Password"), "secret123");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(mockLogin).toHaveBeenCalledWith({
      email: "patient@example.com",
      password: "secret123"
    });

    await waitFor(() => {
      expect(screen.getByText("Patient Route")).toBeInTheDocument();
    });
  });

  it("shows error message when login fails", async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue(new Error("Invalid credentials"));

    renderPage();

    await user.type(screen.getByLabelText("Email"), "patient@example.com");
    await user.type(screen.getByLabelText("Password"), "secret123");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Invalid credentials")).toBeInTheDocument();
  });
});
