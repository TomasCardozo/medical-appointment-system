export function BookingSearchForm({
  doctors,
  doctorId,
  date,
  loadingDoctors,
  loadingSlots,
  minDate,
  onDoctorChange,
  onDateChange,
  onSubmit
}) {
  return (
    <form onSubmit={onSubmit} className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <div className="grid gap-4 md:grid-cols-3">
        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Doctor</span>
          <select
            value={doctorId}
            onChange={(event) => onDoctorChange(event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
            disabled={loadingDoctors}
          >
            {doctors.map((doctor) => (
              <option key={doctor.id} value={doctor.id}>
                {doctor.fullName} - {doctor.specialty}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Date</span>
          <input
            type="date"
            min={minDate}
            value={date}
            onChange={(event) => onDateChange(event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <div className="flex items-end">
          <button
            type="submit"
            disabled={loadingSlots || loadingDoctors}
            className="w-full rounded-lg bg-brand-600 px-4 py-2 font-semibold text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {loadingSlots ? "Searching..." : "Search slots"}
          </button>
        </div>
      </div>
    </form>
  );
}
