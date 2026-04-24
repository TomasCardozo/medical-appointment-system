import { useProfilePage } from "./profile/useProfilePage";

export function ProfilePage() {
  const {
    loading,
    saving,
    error,
    message,
    isDoctor,
    accountForm,
    passwordForm,
    doctorForm,
    handleAccountChange,
    handlePasswordChange,
    handleDoctorChange,
    saveProfile
  } = useProfilePage();

  if (loading) {
    return (
      <div className="rounded-2xl border border-white/90 bg-white/85 p-6 shadow-soft">
        Loading profile...
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <div className="glass-panel rounded-2xl p-6 shadow-soft">
        <h1 className="text-3xl font-extrabold text-brand-700">Profile</h1>
        <p className="mt-2 text-sm text-slate-600">
          {isDoctor
            ? "Update your account, password, and professional profile."
            : "Update your account information and password."}
        </p>
      </div>

      {error && <p className="rounded-lg bg-rose-100 px-4 py-3 text-sm text-rose-700">{error}</p>}
      {message && <p className="rounded-lg bg-emerald-100 px-4 py-3 text-sm text-emerald-700">{message}</p>}

      <form onSubmit={saveProfile} className="space-y-6">
        <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
          <h2 className="text-xl font-bold text-slate-800">Account</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <label className="block md:col-span-2">
              <span className="mb-1 block text-sm font-semibold text-slate-700">Full name</span>
              <input
                type="text"
                value={accountForm.fullName}
                onChange={(event) => handleAccountChange("fullName", event.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
              />
            </label>

            <label className="block">
              <span className="mb-1 block text-sm font-semibold text-slate-700">Email</span>
              <input
                type="email"
                readOnly
                value={accountForm.email}
                className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-slate-600"
              />
            </label>

            <label className="block">
              <span className="mb-1 block text-sm font-semibold text-slate-700">Role</span>
              <input
                type="text"
                readOnly
                value={accountForm.role}
                className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-slate-600"
              />
            </label>
          </div>
        </article>

        <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
          <h2 className="text-xl font-bold text-slate-800">Security</h2>
          <p className="mt-1 text-sm text-slate-600">Complete all fields only if you want to change your password.</p>
          <div className="mt-4 grid gap-4 md:grid-cols-3">
            <label className="block">
              <span className="mb-1 block text-sm font-semibold text-slate-700">Current password</span>
              <input
                type="password"
                value={passwordForm.currentPassword}
                onChange={(event) => handlePasswordChange("currentPassword", event.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
              />
            </label>

            <label className="block">
              <span className="mb-1 block text-sm font-semibold text-slate-700">New password</span>
              <input
                type="password"
                value={passwordForm.newPassword}
                onChange={(event) => handlePasswordChange("newPassword", event.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
              />
            </label>

            <label className="block">
              <span className="mb-1 block text-sm font-semibold text-slate-700">Confirm new password</span>
              <input
                type="password"
                value={passwordForm.confirmNewPassword}
                onChange={(event) => handlePasswordChange("confirmNewPassword", event.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
              />
            </label>
          </div>
        </article>

        {isDoctor && (
          <article className="rounded-2xl border border-white/90 bg-white/85 p-5 shadow-soft">
            <h2 className="text-xl font-bold text-slate-800">Professional profile</h2>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-1 block text-sm font-semibold text-slate-700">Specialty</span>
                <input
                  type="text"
                  value={doctorForm.specialty}
                  onChange={(event) => handleDoctorChange("specialty", event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
                />
              </label>

              <label className="block">
                <span className="mb-1 block text-sm font-semibold text-slate-700">License number</span>
                <input
                  type="text"
                  value={doctorForm.licenseNumber}
                  onChange={(event) => handleDoctorChange("licenseNumber", event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
                />
              </label>

              <label className="block md:col-span-2">
                <span className="mb-1 block text-sm font-semibold text-slate-700">Clinic address</span>
                <input
                  type="text"
                  value={doctorForm.clinicAddress}
                  onChange={(event) => handleDoctorChange("clinicAddress", event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
                />
              </label>

              <label className="block md:col-span-2">
                <span className="mb-1 block text-sm font-semibold text-slate-700">Bio</span>
                <textarea
                  rows={3}
                  value={doctorForm.bio}
                  onChange={(event) => handleDoctorChange("bio", event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-600"
                />
              </label>
            </div>
          </article>
        )}

        <div>
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-brand-600 px-4 py-2 font-semibold text-white transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {saving ? "Saving..." : "Save profile"}
          </button>
        </div>
      </form>
    </section>
  );
}
