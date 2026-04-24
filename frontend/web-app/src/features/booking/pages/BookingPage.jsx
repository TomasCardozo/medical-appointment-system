import { useSearchParams } from "react-router-dom";
import { BookingSearchForm } from "./booking/BookingSearchForm";
import { SelectedDoctorCard } from "./booking/SelectedDoctorCard";
import { SlotsSection } from "./booking/SlotsSection";
import { useBookingPage } from "./booking/useBookingPage";

export function BookingPage() {
  const [searchParams] = useSearchParams();
  const doctorIdFromQuery = searchParams.get("doctorId") || "";
  const {
    doctors,
    doctorId,
    setDoctorId,
    date,
    setDate,
    slots,
    loadingDoctors,
    loadingSlots,
    message,
    error,
    selectedDoctor,
    todayIso,
    searchSlots,
    reserveSlot
  } = useBookingPage(doctorIdFromQuery);

  return (
    <section className="space-y-6">
      <div className="glass-panel rounded-2xl p-6 shadow-soft">
        <h1 className="text-3xl font-extrabold text-brand-700">Book appointment</h1>
        <p className="mt-2 text-sm text-slate-600">Search available slots and reserve in one click.</p>
      </div>

      <BookingSearchForm
        doctors={doctors}
        doctorId={doctorId}
        date={date}
        loadingDoctors={loadingDoctors}
        loadingSlots={loadingSlots}
        minDate={todayIso()}
        onDoctorChange={setDoctorId}
        onDateChange={setDate}
        onSubmit={searchSlots}
      />

      <SelectedDoctorCard doctor={selectedDoctor} />

      {error && <p className="rounded-lg bg-rose-100 px-4 py-3 text-sm text-rose-700">{error}</p>}
      {message && <p className="rounded-lg bg-emerald-100 px-4 py-3 text-sm text-emerald-700">{message}</p>}

      <SlotsSection slots={slots} onReserve={reserveSlot} />
    </section>
  );
}
