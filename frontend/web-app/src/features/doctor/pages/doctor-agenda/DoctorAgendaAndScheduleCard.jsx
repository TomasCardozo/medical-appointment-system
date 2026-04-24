export function DoctorAgendaAndScheduleCard({
  range,
  sortedAgenda,
  schedule,
  deletingAvailabilityId,
  onRangeChange,
  onRefresh,
  onRequestDeleteAvailability
}) {
  function formatTime(value) {
    return typeof value === "string" ? value.slice(0, 5) : value;
  }

  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <h2 className="mr-auto text-xl font-bold text-slate-800">
          Agenda and schedule
        </h2>
        <form onSubmit={onRefresh} className="flex flex-wrap items-end gap-2">
          <label className="block text-sm">
            <span className="mb-1 block font-semibold text-slate-700">
              From
            </span>
            <input
              type="date"
              value={range.from}
              onChange={(event) => onRangeChange("from", event.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block font-semibold text-slate-700">To</span>
            <input
              type="date"
              value={range.to}
              onChange={(event) => onRangeChange("to", event.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
            />
          </label>
          <button
            type="submit"
            className="rounded-lg bg-brand-600 px-4 py-2 font-semibold text-white hover:bg-brand-700"
          >
            Refresh
          </button>
        </form>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div>
          <h3 className="mb-2 text-lg font-bold text-slate-800">
            Booked appointments
          </h3>
          {sortedAgenda.length === 0 ? (
            <p className="text-sm text-slate-600">
              No booked appointments in selected range.
            </p>
          ) : (
            <ul className="space-y-2">
              {sortedAgenda.map((item) => (
                <li
                  key={item.id}
                  className="rounded-lg border border-slate-200 p-3"
                >
                  <p className="font-semibold text-slate-800">
                    {item.appointmentDate} - {formatTime(item.startTime)} to{" "}
                    {formatTime(item.endTime)}
                  </p>
                  <p className="text-xs uppercase tracking-wide text-slate-500">
                    {item.status}
                  </p>
                  <p className="text-sm text-slate-600">
                    {item.patientFullName
                      ? `Patient: ${item.patientFullName}`
                      : `Patient #${item.patientId}`}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div>
          <h3 className="mb-2 text-lg font-bold text-slate-800">
            Weekly availability rules
          </h3>
          {!schedule?.availabilityRules ||
          schedule.availabilityRules.length === 0 ? (
            <p className="text-sm text-slate-600">
              No availability configured yet.
            </p>
          ) : (
            <ul className="space-y-2">
              {schedule.availabilityRules.map((rule) => (
                <li
                  key={rule.id}
                  className="rounded-lg border border-slate-200 p-3 text-sm"
                >
                  <p className="font-semibold text-slate-800">
                    {rule.dayOfWeek}
                  </p>
                  <p className="text-slate-600">
                    {rule.startTime} to {rule.endTime} (
                    {rule.slotDurationMinutes} min)
                  </p>
                  <div className="mt-2">
                    <button
                      type="button"
                      onClick={() => onRequestDeleteAvailability(rule)}
                      disabled={deletingAvailabilityId === rule.id}
                      className="rounded-lg bg-rose-600 px-3 py-1.5 font-semibold text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-70"
                    >
                      {deletingAvailabilityId === rule.id ? "Deleting..." : "Delete"}
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </article>
  );
}
