ALTER TABLE appointments
    ADD COLUMN reminder_requested_at TIMESTAMPTZ;

CREATE INDEX idx_appointments_pending_reminder
    ON appointments (appointment_date, start_time)
    WHERE status = 'BOOKED' AND reminder_requested_at IS NULL;
