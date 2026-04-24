ALTER TABLE appointments
    ADD COLUMN previous_appointment_date DATE,
    ADD COLUMN previous_start_time TIME,
    ADD COLUMN previous_end_time TIME,
    ADD COLUMN reschedule_reason VARCHAR(400),
    ADD COLUMN rescheduled_at TIMESTAMPTZ;
