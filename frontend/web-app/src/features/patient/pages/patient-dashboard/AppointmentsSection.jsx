export function AppointmentsSection({ appointments, loading, onRequestCancel, cancellingAppointmentId }) {
  function formatTime(value) {
    return typeof value === "string" ? value.slice(0, 5) : value;
  }

  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <h2 className="mb-4 text-xl font-bold text-slate-800">My appointments</h2>

      {loading ? (
        <p className="text-sm text-slate-600">Loading appointments...</p>
      ) : appointments.length === 0 ? (
        <p className="text-sm text-slate-600">You have no appointments yet.</p>
      ) : (
        <ul className="space-y-3">
          {appointments.map((appointment) => (
            <li
              key={appointment.id}
              className="rounded-xl border border-slate-200 p-3"
            >
              <p className="font-bold text-slate-800">
                Doctor{" "}
                {appointment.doctorFullName ?? `#${appointment.doctorId}`} |{" "}
                {appointment.appointmentDate}
              </p>
              <p className="text-sm text-slate-600">
                {formatTime(appointment.startTime)}
              </p>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                {appointment.status}
              </p>

              {appointment.status === "BOOKED" && (
                <button
                  type="button"
                  onClick={() => onRequestCancel(appointment)}
                  disabled={cancellingAppointmentId === appointment.id}
                  className="mt-2 rounded-lg bg-rose-600 px-3 py-1 text-xs font-semibold text-white hover:bg-rose-700"
                >
                  {cancellingAppointmentId === appointment.id ? "Cancelling..." : "Cancel"}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
