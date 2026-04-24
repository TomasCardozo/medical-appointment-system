export function DoctorAvailabilityCard({ availabilityForm, dayOptions, onAvailabilityChange, onSubmit }) {
  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <h2 className="text-xl font-bold text-slate-800">Add availability</h2>

      <form onSubmit={onSubmit} className="mt-4 grid gap-3 md:grid-cols-2">
        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Day</span>
          <select
            value={availabilityForm.dayOfWeek}
            onChange={(event) => onAvailabilityChange("dayOfWeek", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          >
            {dayOptions.map((day) => (
              <option key={day} value={day}>
                {day}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Slot duration (minutes)</span>
          <input
            type="number"
            min={15}
            max={240}
            value={availabilityForm.slotDurationMinutes}
            onChange={(event) => onAvailabilityChange("slotDurationMinutes", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Start time</span>
          <input
            type="time"
            value={availabilityForm.startTime}
            onChange={(event) => onAvailabilityChange("startTime", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">End time</span>
          <input
            type="time"
            value={availabilityForm.endTime}
            onChange={(event) => onAvailabilityChange("endTime", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <div className="md:col-span-2">
          <button
            type="submit"
            className="rounded-lg bg-accent-500 px-4 py-2 font-semibold text-white hover:bg-accent-400"
          >
            Save availability
          </button>
        </div>
      </form>
    </article>
  );
}
