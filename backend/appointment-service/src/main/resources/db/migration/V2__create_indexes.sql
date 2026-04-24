CREATE INDEX idx_appointments_patient_id ON appointments (patient_id);
CREATE INDEX idx_appointments_doctor_date ON appointments (doctor_id, appointment_date);
CREATE INDEX idx_appointments_doctor_date_status_start ON appointments (doctor_id, appointment_date, status, start_time);

CREATE UNIQUE INDEX uk_appointments_doctor_slot_booked
    ON appointments (doctor_id, appointment_date, start_time, end_time)
    WHERE status = 'BOOKED';
