import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { RegisterPage } from "./RegisterPage";

const mockRegister = vi.fn();

vi.mock("../AuthContext", () => ({
  useAuth: () => ({
    register: mockRegister
  })
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/register"]}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<div>Login Route</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("RegisterPage", () => {
  beforeEach(() => {
    mockRegister.mockReset();
  });

  it("submits selected role and form data", async () => {
    const user = userEvent.setup();
    mockRegister.mockResolvedValue(undefined);

    renderPage();

    await user.click(screen.getByRole("button", { name: "DOCTOR" }));
    await user.type(screen.getByLabelText("Full name"), "Doctor Demo");
    await user.type(screen.getByLabelText("Email"), "doctor@example.com");
    await user.type(screen.getByLabelText("Password"), "secret123");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        role: "DOCTOR",
        fullName: "Doctor Demo",
        email: "doctor@example.com",
        password: "secret123"
      });
    });

    expect(screen.getByText("Account created successfully. You can now sign in.")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Login Route")).toBeInTheDocument();
    }, { timeout: 2000 });
  });

  it("shows error on failed register", async () => {
    const user = userEvent.setup();
    mockRegister.mockRejectedValue(new Error("Email already exists"));

    renderPage();

    await user.type(screen.getByLabelText("Full name"), "Patient Demo");
    await user.type(screen.getByLabelText("Email"), "patient@example.com");
    await user.type(screen.getByLabelText("Password"), "secret123");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Email already exists")).toBeInTheDocument();
  });
});
