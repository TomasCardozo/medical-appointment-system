import { useCallback, useEffect, useMemo, useState } from "react";
import { apiClient } from "../../../../shared/api/client";
import { getApiErrorMessage } from "../../../../shared/api/errors";

export const DAY_OPTIONS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY"
];

function toIso(date) {
  return date.toISOString().slice(0, 10);
}

function defaultDateRange() {
  const today = new Date();
  const nextWeek = new Date();
  nextWeek.setDate(today.getDate() + 7);
  return { from: toIso(today), to: toIso(nextWeek) };
}

export function useDoctorAgendaPage() {
  const [doctorProfile, setDoctorProfile] = useState(null);
  const [agenda, setAgenda] = useState([]);
  const [schedule, setSchedule] = useState(null);
  const [deletingAvailabilityId, setDeletingAvailabilityId] = useState(null);
  const [pendingAvailabilityDelete, setPendingAvailabilityDelete] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const [profileForm, setProfileForm] = useState({
    fullName: "",
    specialty: "",
    licenseNumber: "",
    clinicAddress: "",
    bio: ""
  });

  const [availabilityForm, setAvailabilityForm] = useState({
    dayOfWeek: "MONDAY",
    startTime: "09:00",
    endTime: "13:00",
    slotDurationMinutes: 30
  });

  const [range, setRange] = useState(defaultDateRange());

  const hasProfile = Boolean(doctorProfile?.id);

  const sortedAgenda = useMemo(
    () => [...agenda].sort((a, b) => `${a.appointmentDate}${a.startTime}`.localeCompare(`${b.appointmentDate}${b.startTime}`)),
    [agenda]
  );

  const loadProfile = useCallback(async () => {
    try {
      const profileResponse = await apiClient.get("/doctors/me");
      setDoctorProfile(profileResponse.data);
      return profileResponse.data;
    } catch (profileError) {
      if (profileError?.response?.status === 404) {
        setDoctorProfile(null);
        return null;
      }
      throw profileError;
    }
  }, []);

  const loadAgendaAndSchedule = useCallback(async (profile, fromValue, toValue) => {
    const doctorId = profile?.id;
    if (!doctorId) {
      setAgenda([]);
      setSchedule(null);
      return;
    }

    const [agendaResponse, scheduleResponse] = await Promise.all([
      apiClient.get(`/appointments/doctor/${doctorId}/agenda`, {
        params: { from: fromValue, to: toValue }
      }),
      apiClient.get(`/doctors/${doctorId}/schedule`, {
        params: { from: fromValue, to: toValue }
      })
    ]);

    setAgenda(agendaResponse.data);
    setSchedule(scheduleResponse.data);
  }, []);

  const reloadPage = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const profile = await loadProfile();
      if (profile?.id) {
        await loadAgendaAndSchedule(profile, range.from, range.to);
      }
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, "Could not load doctor page"));
    } finally {
      setLoading(false);
    }
  }, [loadAgendaAndSchedule, loadProfile, range.from, range.to]);

  useEffect(() => {
    reloadPage();
  }, [reloadPage]);

  const createProfile = useCallback(async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      const response = await apiClient.post("/doctors/profile", profileForm);
      setDoctorProfile(response.data);
      setMessage("Doctor profile created successfully.");
    } catch (submitError) {
      setError(getApiErrorMessage(submitError, "Could not create doctor profile"));
    }
  }, [profileForm]);

  const createAvailability = useCallback(async (event) => {
    event.preventDefault();
    if (!doctorProfile?.id) {
      setError("Doctor profile is required");
      return;
    }

    setError("");
    setMessage("");
    try {
      await apiClient.post(`/doctors/${doctorProfile.id}/availability`, {
        ...availabilityForm,
        slotDurationMinutes: Number(availabilityForm.slotDurationMinutes)
      });
      setMessage("Availability created.");
      await loadAgendaAndSchedule(doctorProfile, range.from, range.to);
    } catch (submitError) {
      setError(getApiErrorMessage(submitError, "Could not create availability"));
    }
  }, [availabilityForm, doctorProfile, loadAgendaAndSchedule, range.from, range.to]);

  const refreshAgenda = useCallback(async (event) => {
    event.preventDefault();
    if (!doctorProfile?.id) {
      return;
    }

    setError("");
    setMessage("");
    try {
      await loadAgendaAndSchedule(doctorProfile, range.from, range.to);
      setMessage("Agenda refreshed.");
    } catch (refreshError) {
      setError(getApiErrorMessage(refreshError, "Could not refresh agenda"));
    }
  }, [doctorProfile, loadAgendaAndSchedule, range.from, range.to]);

  const requestDeleteAvailability = useCallback((availabilityRule) => {
    setError("");
    setMessage("");
    setPendingAvailabilityDelete({
      id: availabilityRule.id,
      dayOfWeek: availabilityRule.dayOfWeek,
      startTime: availabilityRule.startTime,
      endTime: availabilityRule.endTime
    });
  }, []);

  const cancelDeleteAvailability = useCallback(() => {
    setPendingAvailabilityDelete(null);
  }, []);

  const confirmDeleteAvailability = useCallback(async () => {
    const availabilityId = pendingAvailabilityDelete?.id;
    if (!doctorProfile?.id) {
      setError("Doctor profile is required");
      return;
    }
    if (!availabilityId) {
      return;
    }

    setDeletingAvailabilityId(availabilityId);
    setError("");
    setMessage("");
    try {
      await apiClient.delete(`/doctors/${doctorProfile.id}/availability/${availabilityId}`);
      await loadAgendaAndSchedule(doctorProfile, range.from, range.to);
      setPendingAvailabilityDelete(null);
      setMessage("Availability deleted.");
    } catch (deleteError) {
      setError(getApiErrorMessage(deleteError, "Could not delete availability"));
    } finally {
      setDeletingAvailabilityId(null);
    }
  }, [doctorProfile, loadAgendaAndSchedule, pendingAvailabilityDelete, range.from, range.to]);

  return {
    loading,
    error,
    message,
    hasProfile,
    doctorProfile,
    profileForm,
    setProfileForm,
    availabilityForm,
    setAvailabilityForm,
    range,
    setRange,
    sortedAgenda,
    schedule,
    deletingAvailabilityId,
    pendingAvailabilityDelete,
    createProfile,
    createAvailability,
    refreshAgenda,
    requestDeleteAvailability,
    confirmDeleteAvailability,
    cancelDeleteAvailability
  };
}
