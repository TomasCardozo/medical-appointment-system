import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { PatientDashboardPage } from "./PatientDashboardPage";

const mockGet = vi.fn();
const mockPut = vi.fn();

vi.mock("../../auth/AuthContext", () => ({
  useAuth: () => ({
    user: { id: 11, role: "PATIENT" }
  })
}));

vi.mock("../../../shared/api/client", () => ({
  apiClient: {
    get: (...args) => mockGet(...args),
    put: (...args) => mockPut(...args)
  }
}));

describe("PatientDashboardPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPut.mockReset();
  });

  it("loads doctors and appointments and cancels booked appointment", async () => {
    const user = userEvent.setup();

    mockGet
      .mockResolvedValueOnce({
        data: [{ id: 4, fullName: "Dr. House", specialty: "Diagnostics" }]
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 88,
            doctorId: 4,
            appointmentDate: "2026-05-05",
            startTime: "10:00",
            endTime: "10:30",
            status: "BOOKED"
          }
        ]
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 700,
            appointmentId: 88,
            eventType: "appointment.created",
            status: "SENT",
            processedAt: "2026-05-05T10:02:00Z"
          }
        ]
      })
      .mockResolvedValueOnce({
        data: [{ id: 4, fullName: "Dr. House", specialty: "Diagnostics" }]
      })
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({
        data: [
          {
            id: 700,
            appointmentId: 88,
            eventType: "appointment.cancelled",
            status: "SENT",
            processedAt: "2026-05-05T10:15:00Z"
          }
        ]
      });

    mockPut.mockResolvedValue({ data: {} });

    render(
      <MemoryRouter>
        <PatientDashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Dr. House")).toBeInTheDocument();
    expect(screen.getByText("Doctor #4 | 2026-05-05")).toBeInTheDocument();
    expect(screen.getByText("appointment.created")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.getByText("You are about to cancel your appointment with Doctor #4 on 2026-05-05 at 10:00.")).toBeInTheDocument();

    await user.type(screen.getByPlaceholderText("Optional reason"), "Need reschedule");
    await user.click(screen.getByRole("button", { name: "Confirm cancel" }));

    expect(mockPut).toHaveBeenCalledWith("/appointments/88/cancel", {
      cancellationReason: "Need reschedule"
    });

    await waitFor(() => {
      expect(screen.getByText("You have no appointments yet.")).toBeInTheDocument();
    });

    expect(screen.getByText("appointment.cancelled")).toBeInTheDocument();
  });

  it("does not cancel appointment when user keeps appointment", async () => {
    const user = userEvent.setup();

    mockGet
      .mockResolvedValueOnce({
        data: [{ id: 4, fullName: "Dr. House", specialty: "Diagnostics" }]
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 88,
            doctorId: 4,
            appointmentDate: "2026-05-05",
            startTime: "10:00",
            endTime: "10:30",
            status: "BOOKED"
          }
        ]
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 701,
            appointmentId: 88,
            eventType: "appointment.created",
            status: "SENT",
            processedAt: "2026-05-05T10:02:00Z"
          }
        ]
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 702,
            appointmentId: 88,
            eventType: "appointment.reminder.requested",
            status: "SENT",
            processedAt: "2026-05-05T10:20:00Z"
          }
        ]
      });

    render(
      <MemoryRouter>
        <PatientDashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Doctor #4 | 2026-05-05")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cancel" }));
    await user.click(screen.getByRole("button", { name: "Keep appointment" }));
    await user.click(screen.getByRole("button", { name: "Refresh notifications" }));

    expect(mockPut).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: "Confirm cancel" })).not.toBeInTheDocument();
    expect(screen.getByText("appointment.reminder.requested")).toBeInTheDocument();
  });
});
