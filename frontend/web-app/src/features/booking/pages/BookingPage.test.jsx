import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { BookingPage } from "./BookingPage";

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock("../../../shared/api/client", () => ({
  apiClient: {
    get: (...args) => mockGet(...args),
    post: (...args) => mockPost(...args)
  }
}));

describe("BookingPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
  });

  it("loads doctors, searches slots and reserves appointment", async () => {
    const user = userEvent.setup();

    mockGet
      .mockResolvedValueOnce({
        data: [
          { id: 2, fullName: "Dr. Quinn", specialty: "Family" },
          { id: 3, fullName: "Dr. Cox", specialty: "Internal" }
        ]
      })
      .mockResolvedValueOnce({
        data: [
          {
            doctorId: 2,
            date: "2026-05-06",
            startTime: "09:00",
            endTime: "09:30",
            slotDurationMinutes: 30
          }
        ]
      })
      .mockResolvedValueOnce({
        data: []
      });

    mockPost.mockResolvedValue({ data: {} });

    render(
      <MemoryRouter initialEntries={["/booking?doctorId=2"]}>
        <BookingPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Dr. Quinn")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Search slots" }));

    expect(mockGet).toHaveBeenLastCalledWith("/appointments/available", {
      params: expect.objectContaining({ doctorId: "2" })
    });

    expect(await screen.findByRole("button", { name: "Reserve" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Reserve" }));

    expect(mockPost).toHaveBeenCalledWith("/appointments", {
      doctorId: 2,
      appointmentDate: "2026-05-06",
      startTime: "09:00"
    });

    await waitFor(() => {
      expect(screen.getByText("No slots available for selected date.")).toBeInTheDocument();
    });
  });
});
