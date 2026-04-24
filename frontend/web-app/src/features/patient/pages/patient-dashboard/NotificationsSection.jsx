function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function statusBadgeClass(status) {
  if (status === "SENT") {
    return "bg-emerald-100 text-emerald-700";
  }
  if (status === "FAILED") {
    return "bg-rose-100 text-rose-700";
  }
  return "bg-slate-100 text-slate-700";
}

export function NotificationsSection({ notifications, loading, refreshing, error, onRefresh }) {
  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Notifications</h2>
          <p className="text-xs text-slate-500">Kafka events processed by notification-service.</p>
        </div>
        <button
          type="button"
          onClick={onRefresh}
          disabled={refreshing}
          className="rounded-lg border border-brand-600 px-3 py-1 text-xs font-semibold text-brand-700 hover:bg-brand-100 disabled:cursor-not-allowed disabled:opacity-70"
        >
          {refreshing ? "Refreshing..." : "Refresh notifications"}
        </button>
      </div>

      {error && <p className="mb-3 rounded-lg bg-amber-100 px-3 py-2 text-xs text-amber-800">{error}</p>}

      {loading ? (
        <p className="text-sm text-slate-600">Loading notifications...</p>
      ) : notifications.length === 0 ? (
        <p className="text-sm text-slate-600">No notifications yet.</p>
      ) : (
        <ul className="space-y-3">
          {notifications.map((notification) => (
            <li key={notification.id} className="rounded-xl border border-slate-200 p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="font-semibold text-slate-800">{notification.eventType}</p>
                <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${statusBadgeClass(notification.status)}`}>
                  {notification.status}
                </span>
              </div>
              <p className="mt-1 text-xs text-slate-600">Appointment #{notification.appointmentId}</p>
              <p className="text-xs text-slate-500">Processed: {formatDateTime(notification.processedAt)}</p>
              {notification.errorMessage && <p className="mt-1 text-xs text-rose-700">{notification.errorMessage}</p>}
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
