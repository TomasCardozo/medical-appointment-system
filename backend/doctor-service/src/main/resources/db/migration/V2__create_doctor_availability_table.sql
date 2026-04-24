CREATE TABLE doctor_availabilities (
    id BIGSERIAL PRIMARY KEY,
    doctor_profile_id BIGINT NOT NULL,
    day_of_week VARCHAR(12) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_doctor_availabilities_profile
        FOREIGN KEY (doctor_profile_id)
        REFERENCES doctor_profiles (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_doctor_availabilities_duration_positive
        CHECK (slot_duration_minutes > 0),
    CONSTRAINT chk_doctor_availabilities_start_end
        CHECK (start_time < end_time)
);

CREATE INDEX idx_doctor_availabilities_profile_day_start
    ON doctor_availabilities (doctor_profile_id, day_of_week, start_time);

CREATE INDEX idx_doctor_availabilities_profile_day
    ON doctor_availabilities (doctor_profile_id, day_of_week);
