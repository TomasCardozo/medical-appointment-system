export function DoctorProfileSummaryCard({ doctorProfile }) {
  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <h2 className="text-xl font-bold text-slate-800">Profile</h2>
      <p className="mt-2 font-semibold text-slate-800">{doctorProfile.fullName}</p>
      <p className="text-sm text-slate-600">{doctorProfile.specialty}</p>
      <p className="mt-1 text-sm text-slate-600">License: {doctorProfile.licenseNumber}</p>
      <p className="mt-1 text-sm text-slate-600">{doctorProfile.clinicAddress || "No clinic address"}</p>
    </article>
  );
}
