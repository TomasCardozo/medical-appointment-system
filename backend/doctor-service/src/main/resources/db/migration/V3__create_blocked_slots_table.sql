CREATE TABLE blocked_slots (
    id BIGSERIAL PRIMARY KEY,
    doctor_profile_id BIGINT NOT NULL,
    blocked_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_blocked_slots_profile
        FOREIGN KEY (doctor_profile_id)
        REFERENCES doctor_profiles (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_blocked_slots_start_end
        CHECK (start_time < end_time)
);

CREATE INDEX idx_blocked_slots_profile_date_start
    ON blocked_slots (doctor_profile_id, blocked_date, start_time);

CREATE INDEX idx_blocked_slots_profile_date
    ON blocked_slots (doctor_profile_id, blocked_date);
