CREATE TABLE doctor_profiles (
    id BIGSERIAL PRIMARY KEY,
    owner_email VARCHAR(180) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    specialty VARCHAR(120) NOT NULL,
    license_number VARCHAR(60) NOT NULL,
    clinic_address VARCHAR(220),
    bio VARCHAR(800),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_doctor_profiles_owner_email UNIQUE (owner_email),
    CONSTRAINT uk_doctor_profiles_license_number UNIQUE (license_number)
);

CREATE INDEX idx_doctor_profiles_specialty ON doctor_profiles (specialty);
CREATE INDEX idx_doctor_profiles_active ON doctor_profiles (active);
