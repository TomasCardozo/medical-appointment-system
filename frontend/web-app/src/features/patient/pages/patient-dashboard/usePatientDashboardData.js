import { useCallback, useEffect, useState } from "react";
import { apiClient } from "../../../../shared/api/client";
import { getApiErrorMessage } from "../../../../shared/api/errors";

export function usePatientDashboardData(userId) {
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [pendingAppointmentCancellation, setPendingAppointmentCancellation] = useState(null);
  const [cancellingAppointmentId, setCancellingAppointmentId] = useState(null);
  const [refreshingNotifications, setRefreshingNotifications] = useState(false);
  const [notificationsError, setNotificationsError] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadNotifications = useCallback(
    async ({ silent } = { silent: false }) => {
      if (!userId) {
        return;
      }

      if (!silent) {
        setRefreshingNotifications(true);
      }
      setNotificationsError("");

      try {
        const notificationResponse = await apiClient.get("/notifications");
        setNotifications(notificationResponse.data);
      } catch (loadError) {
        setNotificationsError(getApiErrorMessage(loadError, "Could not load notifications"));
      } finally {
        if (!silent) {
          setRefreshingNotifications(false);
        }
      }
    },
    [userId]
  );

  const loadData = useCallback(async () => {
    if (!userId) {
      return;
    }

    setLoading(true);
    setError("");
    try {
      const [doctorResponse, appointmentResponse, notificationResponse] = await Promise.all([
        apiClient.get("/doctors"),
        apiClient.get(`/appointments/patient/${userId}`),
        apiClient.get("/notifications")
      ]);
      setDoctors(doctorResponse.data);
      setAppointments(appointmentResponse.data);
      setNotifications(notificationResponse.data);
      setNotificationsError("");
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, "Could not load dashboard data"));
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (!userId) {
      return undefined;
    }

    const intervalId = setInterval(() => {
      loadNotifications({ silent: true });
    }, 15000);

    return () => {
      clearInterval(intervalId);
    };
  }, [loadNotifications, userId]);

  const requestCancelAppointment = useCallback((appointment) => {
    setError("");
    setMessage("");
    setPendingAppointmentCancellation({
      id: appointment.id,
      doctorId: appointment.doctorId,
      doctorFullName: appointment.doctorFullName,
      appointmentDate: appointment.appointmentDate,
      startTime: appointment.startTime,
      cancellationReason: ""
    });
  }, []);

  const updatePendingCancellationReason = useCallback((value) => {
    setPendingAppointmentCancellation((prev) => {
      if (!prev) {
        return prev;
      }
      return {
        ...prev,
        cancellationReason: value
      };
    });
  }, []);

  const dismissPendingCancellation = useCallback(() => {
    setPendingAppointmentCancellation(null);
  }, []);

  const confirmCancelAppointment = useCallback(async () => {
    const appointmentId = pendingAppointmentCancellation?.id;
    if (!appointmentId) {
      return;
    }

    setCancellingAppointmentId(appointmentId);
    setError("");
    setMessage("");
    try {
      const cancellationReason = pendingAppointmentCancellation.cancellationReason;
      await apiClient.put(`/appointments/${appointmentId}/cancel`, {
        cancellationReason: cancellationReason || null
      });
      await loadData();
      setPendingAppointmentCancellation(null);
      setMessage("Appointment cancelled.");
    } catch (cancelError) {
      setError(getApiErrorMessage(cancelError, "Could not cancel appointment"));
    } finally {
      setCancellingAppointmentId(null);
    }
  }, [loadData, pendingAppointmentCancellation]);

  return {
    doctors,
    appointments,
    notifications,
    pendingAppointmentCancellation,
    cancellingAppointmentId,
    refreshingNotifications,
    notificationsError,
    loading,
    error,
    message,
    loadData,
    refreshNotifications: loadNotifications,
    requestCancelAppointment,
    updatePendingCancellationReason,
    dismissPendingCancellation,
    confirmCancelAppointment
  };
}
