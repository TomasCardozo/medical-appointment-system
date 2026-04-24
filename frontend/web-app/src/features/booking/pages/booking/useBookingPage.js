import { useEffect, useMemo, useState } from "react";
import { apiClient } from "../../../../shared/api/client";
import { getApiErrorMessage } from "../../../../shared/api/errors";

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export function useBookingPage(initialDoctorId) {
  const [doctors, setDoctors] = useState([]);
  const [doctorId, setDoctorId] = useState(initialDoctorId);
  const [date, setDate] = useState(todayIso());
  const [slots, setSlots] = useState([]);
  const [loadingDoctors, setLoadingDoctors] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const selectedDoctor = useMemo(
    () => doctors.find((doctor) => String(doctor.id) === String(doctorId)),
    [doctors, doctorId]
  );

  useEffect(() => {
    async function loadDoctors() {
      setLoadingDoctors(true);
      try {
        const response = await apiClient.get("/doctors");
        setDoctors(response.data);
        if (!initialDoctorId && response.data.length > 0) {
          setDoctorId(String(response.data[0].id));
        }
      } catch (loadError) {
        setError(getApiErrorMessage(loadError, "Could not load doctors"));
      } finally {
        setLoadingDoctors(false);
      }
    }

    loadDoctors();
  }, [initialDoctorId]);

  async function searchSlots(event) {
    event.preventDefault();
    if (!doctorId || !date) {
      setError("Doctor and date are required");
      return;
    }

    setLoadingSlots(true);
    setError("");
    setMessage("");
    setSlots([]);

    try {
      const response = await apiClient.get("/appointments/available", {
        params: { doctorId, date }
      });
      setSlots(response.data);
      if (response.data.length === 0) {
        setMessage("No slots available for selected date.");
      }
    } catch (slotError) {
      setError(getApiErrorMessage(slotError, "Could not fetch slots"));
    } finally {
      setLoadingSlots(false);
    }
  }

  async function reserveSlot(slot) {
    setError("");
    setMessage("");
    try {
      await apiClient.post("/appointments", {
        doctorId: slot.doctorId,
        appointmentDate: slot.date,
        startTime: slot.startTime
      });
      setMessage("Appointment booked successfully.");
      await searchSlots({ preventDefault() {} });
    } catch (bookingError) {
      setError(getApiErrorMessage(bookingError, "Could not create appointment"));
    }
  }

  return {
    doctors,
    doctorId,
    setDoctorId,
    date,
    setDate,
    slots,
    loadingDoctors,
    loadingSlots,
    message,
    error,
    selectedDoctor,
    todayIso,
    searchSlots,
    reserveSlot
  };
}
