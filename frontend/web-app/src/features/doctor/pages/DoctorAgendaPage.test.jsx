import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DoctorAgendaPage } from "./DoctorAgendaPage";

const mockGet = vi.fn();
const mockPost = vi.fn();
const mockDelete = vi.fn();

vi.mock("../../../shared/api/client", () => ({
  apiClient: {
    get: (...args) => mockGet(...args),
    post: (...args) => mockPost(...args),
    delete: (...args) => mockDelete(...args)
  }
}));

describe("DoctorAgendaPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockDelete.mockReset();
  });

  it("shows onboarding form when doctor profile does not exist", async () => {
    mockGet.mockRejectedValueOnce({ response: { status: 404 } });

    render(<DoctorAgendaPage />);

    expect(await screen.findByText("Create doctor profile")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create profile" })).toBeInTheDocument();
  });

  it("loads agenda and schedule for existing profile and creates availability", async () => {
    const user = userEvent.setup();
    const profile = {
      id: 7,
      fullName: "Dr. Meredith Grey",
      specialty: "General Surgery",
      licenseNumber: "LIC-123",
      clinicAddress: "Seattle"
    };

    mockGet
      .mockResolvedValueOnce({ data: profile })
      .mockResolvedValueOnce({
        data: [
          {
            id: 10,
            appointmentDate: "2026-05-08",
            startTime: "11:00",
            endTime: "11:30",
            status: "BOOKED",
            patientId: 55,
            patientFullName: "Alice Patient"
          }
        ]
      })
      .mockResolvedValueOnce({
        data: {
          availabilityRules: [
            {
              id: 1,
              dayOfWeek: "MONDAY",
              startTime: "09:00",
              endTime: "13:00",
              slotDurationMinutes: 30
            }
          ]
        }
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 10,
            appointmentDate: "2026-05-08",
            startTime: "11:00",
            endTime: "11:30",
            status: "BOOKED",
            patientId: 55,
            patientFullName: "Alice Patient"
          }
        ]
      })
      .mockResolvedValueOnce({
        data: {
          availabilityRules: [
            {
              id: 1,
              dayOfWeek: "MONDAY",
              startTime: "09:00",
              endTime: "13:00",
              slotDurationMinutes: 30
            }
          ]
        }
      });

    mockPost.mockResolvedValue({ data: {} });

    render(<DoctorAgendaPage />);

    expect(await screen.findByText("Dr. Meredith Grey")).toBeInTheDocument();
    expect(screen.getByText("Booked appointments")).toBeInTheDocument();
    expect(screen.getByText("Patient: Alice Patient")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Save availability" }));

    expect(mockPost).toHaveBeenCalledWith("/doctors/7/availability", {
      dayOfWeek: "MONDAY",
      startTime: "09:00",
      endTime: "13:00",
      slotDurationMinutes: 30
    });

    await waitFor(() => {
      expect(screen.getByText("Availability created.")).toBeInTheDocument();
    });
  });

  it("deletes availability after confirmation", async () => {
    const user = userEvent.setup();
    const profile = {
      id: 7,
      fullName: "Dr. Meredith Grey",
      specialty: "General Surgery",
      licenseNumber: "LIC-123",
      clinicAddress: "Seattle"
    };

    mockGet
      .mockResolvedValueOnce({ data: profile })
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({
        data: {
          availabilityRules: [
            {
              id: 1,
              dayOfWeek: "MONDAY",
              startTime: "09:00",
              endTime: "13:00",
              slotDurationMinutes: 30
            }
          ]
        }
      })
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({
        data: {
          availabilityRules: []
        }
      });

    mockDelete.mockResolvedValue({ data: {} });

    render(<DoctorAgendaPage />);

    expect(await screen.findByRole("button", { name: "Delete" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Delete" }));
    expect(screen.getByText("You are about to delete availability MONDAY 09:00 to 13:00.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Confirm delete" }));

    expect(mockDelete).toHaveBeenCalledWith("/doctors/7/availability/1");

    await waitFor(() => {
      expect(screen.getByText("Availability deleted.")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText("No availability configured yet.")).toBeInTheDocument();
    });
  });

  it("does not delete availability when confirmation is cancelled", async () => {
    const user = userEvent.setup();
    const profile = {
      id: 7,
      fullName: "Dr. Meredith Grey",
      specialty: "General Surgery",
      licenseNumber: "LIC-123",
      clinicAddress: "Seattle"
    };

    mockGet
      .mockResolvedValueOnce({ data: profile })
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({
        data: {
          availabilityRules: [
            {
              id: 1,
              dayOfWeek: "MONDAY",
              startTime: "09:00",
              endTime: "13:00",
              slotDurationMinutes: 30
            }
          ]
        }
      });

    render(<DoctorAgendaPage />);

    expect(await screen.findByRole("button", { name: "Delete" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Delete" }));
    expect(screen.getByText("You are about to delete availability MONDAY 09:00 to 13:00.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cancel" }));

    expect(mockDelete).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: "Confirm delete" })).not.toBeInTheDocument();
  });
});
