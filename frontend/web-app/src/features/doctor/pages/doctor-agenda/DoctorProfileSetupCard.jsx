export function DoctorProfileSetupCard({ profileForm, onProfileChange, onSubmit }) {
  return (
    <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
      <h2 className="text-xl font-bold text-slate-800">Create doctor profile</h2>
      <p className="mt-1 text-sm text-slate-600">First login as doctor, then create profile to enable schedule setup.</p>

      <form onSubmit={onSubmit} className="mt-4 grid gap-4 md:grid-cols-2">
        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Full name</span>
          <input
            type="text"
            required
            value={profileForm.fullName}
            onChange={(event) => onProfileChange("fullName", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Specialty</span>
          <input
            type="text"
            required
            value={profileForm.specialty}
            onChange={(event) => onProfileChange("specialty", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">License number</span>
          <input
            type="text"
            required
            value={profileForm.licenseNumber}
            onChange={(event) => onProfileChange("licenseNumber", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Clinic address</span>
          <input
            type="text"
            value={profileForm.clinicAddress}
            onChange={(event) => onProfileChange("clinicAddress", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Bio</span>
          <textarea
            value={profileForm.bio}
            onChange={(event) => onProfileChange("bio", event.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
            rows={3}
          />
        </label>

        <div className="md:col-span-2">
          <button
            type="submit"
            className="rounded-lg bg-brand-600 px-4 py-2 font-semibold text-white hover:bg-brand-700"
          >
            Create profile
          </button>
        </div>
      </form>
    </article>
  );
}
