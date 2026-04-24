export function SlotsSection({ slots, onReserve }) {
  return (
    <div className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <h2 className="mb-4 text-xl font-bold text-slate-800">Available slots</h2>

      {slots.length === 0 ? (
        <p className="text-sm text-slate-600">Run a search to see slots.</p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {slots.map((slot) => (
            <li key={`${slot.date}-${slot.startTime}`} className="rounded-xl border border-slate-200 p-3">
              <p className="font-bold text-slate-800">{slot.date}</p>
              <p className="text-sm text-slate-600">
                {slot.startTime} to {slot.endTime}
              </p>
              <p className="text-xs text-slate-500">{slot.slotDurationMinutes} minutes</p>

              <button
                type="button"
                onClick={() => onReserve(slot)}
                className="mt-3 w-full rounded-lg bg-accent-500 px-3 py-2 text-sm font-semibold text-white hover:bg-accent-400"
              >
                Reserve
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
