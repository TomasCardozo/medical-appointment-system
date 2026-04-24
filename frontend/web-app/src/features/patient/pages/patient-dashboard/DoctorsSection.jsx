import { Link } from "react-router-dom";

export function DoctorsSection({ doctors, loading, onRefresh }) {
  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Doctors</h2>
        <button
          type="button"
          onClick={onRefresh}
          className="rounded-lg border border-brand-600 px-3 py-1 text-xs font-semibold text-brand-700 hover:bg-brand-100"
        >
          Refresh
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-600">Loading doctors...</p>
      ) : doctors.length === 0 ? (
        <p className="text-sm text-slate-600">No doctors available yet.</p>
      ) : (
        <ul className="space-y-3">
          {doctors.map((doctor) => (
            <li key={doctor.id} className="rounded-xl border border-slate-200 p-3">
              <p className="font-bold text-slate-800">{doctor.fullName}</p>
              <p className="text-sm text-slate-600">{doctor.specialty}</p>
              <Link
                to={`/booking?doctorId=${doctor.id}`}
                className="mt-2 inline-block text-sm font-semibold text-brand-700 underline-offset-4 hover:underline"
              >
                Search slots
              </Link>
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
