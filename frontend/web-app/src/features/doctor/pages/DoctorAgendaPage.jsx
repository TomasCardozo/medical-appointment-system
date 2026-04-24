import { DoctorAgendaAndScheduleCard } from "./doctor-agenda/DoctorAgendaAndScheduleCard";
import { DoctorAvailabilityCard } from "./doctor-agenda/DoctorAvailabilityCard";
import { DoctorProfileSetupCard } from "./doctor-agenda/DoctorProfileSetupCard";
import { DoctorProfileSummaryCard } from "./doctor-agenda/DoctorProfileSummaryCard";
import { DAY_OPTIONS, useDoctorAgendaPage } from "./doctor-agenda/useDoctorAgendaPage";

export function DoctorAgendaPage() {
  const {
    loading,
    error,
    message,
    hasProfile,
    doctorProfile,
    profileForm,
    setProfileForm,
    availabilityForm,
    setAvailabilityForm,
    range,
    setRange,
    sortedAgenda,
    schedule,
    deletingAvailabilityId,
    pendingAvailabilityDelete,
    createProfile,
    createAvailability,
    refreshAgenda,
    requestDeleteAvailability,
    confirmDeleteAvailability,
    cancelDeleteAvailability
  } = useDoctorAgendaPage();

  function handleProfileChange(field, value) {
    setProfileForm((prev) => ({ ...prev, [field]: value }));
  }

  function handleAvailabilityChange(field, value) {
    setAvailabilityForm((prev) => ({ ...prev, [field]: value }));
  }

  function handleRangeChange(field, value) {
    setRange((prev) => ({ ...prev, [field]: value }));
  }

  if (loading) {
    return (
      <div className="rounded-2xl border border-white/90 bg-white/85 p-6 shadow-soft">
        Loading doctor page...
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <div className="glass-panel rounded-2xl p-6 shadow-soft">
        <h1 className="text-3xl font-extrabold text-brand-700">Doctor agenda</h1>
        <p className="mt-2 text-sm text-slate-600">Manage profile, configure weekly availability, and check booked appointments.</p>
      </div>

      {error && <p className="rounded-lg bg-rose-100 px-4 py-3 text-sm text-rose-700">{error}</p>}
      {message && <p className="rounded-lg bg-emerald-100 px-4 py-3 text-sm text-emerald-700">{message}</p>}
      {pendingAvailabilityDelete && (
        <div className="flex flex-wrap items-center gap-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          <p className="mr-auto">
            You are about to delete availability {pendingAvailabilityDelete.dayOfWeek} {pendingAvailabilityDelete.startTime} to {pendingAvailabilityDelete.endTime}.
          </p>
          <button
            type="button"
            onClick={confirmDeleteAvailability}
            disabled={deletingAvailabilityId === pendingAvailabilityDelete.id}
            className="rounded-lg bg-rose-600 px-3 py-1.5 font-semibold text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {deletingAvailabilityId === pendingAvailabilityDelete.id ? "Deleting..." : "Confirm delete"}
          </button>
          <button
            type="button"
            onClick={cancelDeleteAvailability}
            disabled={deletingAvailabilityId === pendingAvailabilityDelete.id}
            className="rounded-lg border border-amber-500 bg-white px-3 py-1.5 font-semibold text-amber-900 hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-70"
          >
            Cancel
          </button>
        </div>
      )}

      {!hasProfile && (
        <DoctorProfileSetupCard
          profileForm={profileForm}
          onProfileChange={handleProfileChange}
          onSubmit={createProfile}
        />
      )}

      {hasProfile && (
        <>
          <div className="grid gap-6 lg:grid-cols-2">
            <DoctorProfileSummaryCard doctorProfile={doctorProfile} />
            <DoctorAvailabilityCard
              availabilityForm={availabilityForm}
              dayOptions={DAY_OPTIONS}
              onAvailabilityChange={handleAvailabilityChange}
              onSubmit={createAvailability}
            />
          </div>

          <DoctorAgendaAndScheduleCard
            range={range}
            sortedAgenda={sortedAgenda}
            schedule={schedule}
            deletingAvailabilityId={deletingAvailabilityId}
            onRangeChange={handleRangeChange}
            onRefresh={refreshAgenda}
            onRequestDeleteAvailability={requestDeleteAvailability}
          />
        </>
      )}
    </section>
  );
}
