export function SelectedDoctorCard({ doctor }) {
  if (!doctor) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-accent-100 bg-white/80 p-4">
      <p className="text-sm text-slate-600">Selected doctor</p>
      <p className="font-bold text-slate-800">{doctor.fullName}</p>
      <p className="text-sm text-slate-600">{doctor.specialty}</p>
    </div>
  );
}
