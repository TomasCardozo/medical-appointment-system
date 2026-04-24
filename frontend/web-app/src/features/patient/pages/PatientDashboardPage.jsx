import { useAuth } from "../../auth/AuthContext";
import { AppointmentsSection } from "./patient-dashboard/AppointmentsSection";
import { DoctorsSection } from "./patient-dashboard/DoctorsSection";
import { NotificationsSection } from "./patient-dashboard/NotificationsSection";
import { usePatientDashboardData } from "./patient-dashboard/usePatientDashboardData";

export function PatientDashboardPage() {
  const { user } = useAuth();
  const {
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
    refreshNotifications,
    requestCancelAppointment,
    updatePendingCancellationReason,
    dismissPendingCancellation,
    confirmCancelAppointment
  } = usePatientDashboardData(user?.id);

  function formatTime(value) {
    return typeof value === "string" ? value.slice(0, 5) : value;
  }

  const doctorLabel = pendingAppointmentCancellation?.doctorFullName
    ? pendingAppointmentCancellation.doctorFullName
    : pendingAppointmentCancellation
      ? `Doctor #${pendingAppointmentCancellation.doctorId}`
      : "";

  return (
    <section className="space-y-6">
      <div className="glass-panel rounded-2xl p-6 shadow-soft">
        <h1 className="text-3xl font-extrabold text-brand-700">Patient dashboard</h1>
        <p className="mt-2 text-sm text-slate-600">Manage bookings and find doctors with available slots.</p>
      </div>

      {error && <p className="rounded-lg bg-rose-100 px-4 py-3 text-sm text-rose-700">{error}</p>}
      {message && <p className="rounded-lg bg-emerald-100 px-4 py-3 text-sm text-emerald-700">{message}</p>}
      {pendingAppointmentCancellation && (
        <div className="space-y-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          <p>
            You are about to cancel your appointment with {doctorLabel} on {pendingAppointmentCancellation.appointmentDate} at{" "}
            {formatTime(pendingAppointmentCancellation.startTime)}.
          </p>
          <label className="block">
            <span className="mb-1 block font-semibold text-amber-900">Cancellation reason (optional)</span>
            <input
              type="text"
              value={pendingAppointmentCancellation.cancellationReason}
              onChange={(event) => updatePendingCancellationReason(event.target.value)}
              placeholder="Optional reason"
              className="w-full rounded-lg border border-amber-300 bg-white px-3 py-2 outline-none focus:border-amber-500"
            />
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={confirmCancelAppointment}
              disabled={cancellingAppointmentId === pendingAppointmentCancellation.id}
              className="rounded-lg bg-rose-600 px-3 py-1.5 font-semibold text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {cancellingAppointmentId === pendingAppointmentCancellation.id ? "Cancelling..." : "Confirm cancel"}
            </button>
            <button
              type="button"
              onClick={dismissPendingCancellation}
              disabled={cancellingAppointmentId === pendingAppointmentCancellation.id}
              className="rounded-lg border border-amber-500 bg-white px-3 py-1.5 font-semibold text-amber-900 hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-70"
            >
              Keep appointment
            </button>
          </div>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <DoctorsSection doctors={doctors} loading={loading} onRefresh={loadData} />
        <AppointmentsSection
          appointments={appointments}
          loading={loading}
          cancellingAppointmentId={cancellingAppointmentId}
          onRequestCancel={requestCancelAppointment}
        />
      </div>

      <NotificationsSection
        notifications={notifications}
        loading={loading}
        refreshing={refreshingNotifications}
        error={notificationsError}
        onRefresh={refreshNotifications}
      />
    </section>
  );
}
