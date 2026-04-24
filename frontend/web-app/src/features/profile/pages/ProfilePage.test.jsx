import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ProfilePage } from "./ProfilePage";

const mockGet = vi.fn();
const mockPut = vi.fn();
const mockRefreshMe = vi.fn();
const mockUseAuth = vi.fn();

vi.mock("../../auth/AuthContext", () => ({
  useAuth: () => mockUseAuth()
}));

vi.mock("../../../shared/api/client", () => ({
  apiClient: {
    get: (...args) => mockGet(...args),
    put: (...args) => mockPut(...args)
  }
}));

describe("ProfilePage", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPut.mockReset();
    mockRefreshMe.mockReset();
    mockRefreshMe.mockResolvedValue({});
  });

  it("renders patient profile with readonly email", async () => {
    mockUseAuth.mockReturnValue({
      user: { fullName: "Alice Patient", email: "alice@example.com", role: "PATIENT" },
      refreshMe: mockRefreshMe
    });

    render(<ProfilePage />);

    expect(await screen.findByRole("heading", { name: "Profile" })).toBeInTheDocument();
    expect(screen.queryByText("Professional profile")).not.toBeInTheDocument();

    const emailInput = screen.getByLabelText("Email");
    expect(emailInput).toHaveValue("alice@example.com");
    expect(emailInput).toHaveAttribute("readonly");
  });

  it("submits patient profile update through auth endpoint", async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      user: { fullName: "Alice Patient", email: "alice@example.com", role: "PATIENT" },
      refreshMe: mockRefreshMe
    });
    mockPut.mockResolvedValue({ data: {} });

    render(<ProfilePage />);

    const fullNameInput = await screen.findByLabelText("Full name");
    await user.clear(fullNameInput);
    await user.type(fullNameInput, "Alice Updated");
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    expect(mockPut).toHaveBeenCalledWith("/auth/me", { fullName: "Alice Updated" });
    expect(mockRefreshMe).toHaveBeenCalledTimes(1);
    await waitFor(() => {
      expect(screen.getByText("Profile updated successfully.")).toBeInTheDocument();
    });
  });

  it("shows validation error when password confirmation does not match", async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      user: { fullName: "Alice Patient", email: "alice@example.com", role: "PATIENT" },
      refreshMe: mockRefreshMe
    });

    render(<ProfilePage />);

    await user.type(await screen.findByLabelText("Current password"), "OldPassword123");
    await user.type(screen.getByLabelText("New password"), "NewPassword123");
    await user.type(screen.getByLabelText("Confirm new password"), "MismatchPassword");
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    expect(mockPut).not.toHaveBeenCalled();
    expect(screen.getByText("New password and confirmation must match")).toBeInTheDocument();
  });

  it("submits doctor profile updates to auth and doctor endpoints", async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      user: { fullName: "Dr. Grey", email: "doctor@example.com", role: "DOCTOR" },
      refreshMe: mockRefreshMe
    });
    mockGet.mockResolvedValueOnce({
      data: {
        id: 7,
        fullName: "Dr. Grey",
        specialty: "General Surgery",
        licenseNumber: "LIC-123",
        clinicAddress: "Seattle",
        bio: "Bio"
      }
    });
    mockPut.mockResolvedValue({ data: {} });

    render(<ProfilePage />);

    const fullNameInput = await screen.findByLabelText("Full name");
    await user.clear(fullNameInput);
    await user.type(fullNameInput, "Dr. Meredith Updated");

    const specialtyInput = screen.getByLabelText("Specialty");
    await user.clear(specialtyInput);
    await user.type(specialtyInput, "Internal Medicine");

    await user.click(screen.getByRole("button", { name: "Save profile" }));

    expect(mockPut).toHaveBeenNthCalledWith(1, "/auth/me", { fullName: "Dr. Meredith Updated" });
    expect(mockPut).toHaveBeenNthCalledWith(2, "/doctors/me", {
      fullName: "Dr. Meredith Updated",
      specialty: "Internal Medicine",
      licenseNumber: "LIC-123",
      clinicAddress: "Seattle",
      bio: "Bio"
    });
    expect(mockRefreshMe).toHaveBeenCalledTimes(1);
  });

  it("shows partial save error when doctor profile update fails", async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      user: { fullName: "Dr. Grey", email: "doctor@example.com", role: "DOCTOR" },
      refreshMe: mockRefreshMe
    });
    mockGet.mockResolvedValueOnce({
      data: {
        id: 7,
        fullName: "Dr. Grey",
        specialty: "General Surgery",
        licenseNumber: "LIC-123",
        clinicAddress: "Seattle",
        bio: "Bio"
      }
    });
    mockPut
      .mockResolvedValueOnce({ data: {} })
      .mockRejectedValueOnce(new Error("doctor-service failure"));

    render(<ProfilePage />);

    const fullNameInput = await screen.findByLabelText("Full name");
    await user.clear(fullNameInput);
    await user.type(fullNameInput, "Dr. Meredith Updated");
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    await waitFor(() => {
      expect(screen.getByText("Base profile updated, but doctor profile could not be saved.")).toBeInTheDocument();
    });
    expect(mockRefreshMe).toHaveBeenCalledTimes(1);
  });
});
