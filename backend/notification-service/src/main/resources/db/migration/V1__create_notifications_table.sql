CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    recipient_email VARCHAR(255),
    provider VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(1000),
    processed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_notifications_dedup
    ON notifications (appointment_id, event_type, recipient_email);
