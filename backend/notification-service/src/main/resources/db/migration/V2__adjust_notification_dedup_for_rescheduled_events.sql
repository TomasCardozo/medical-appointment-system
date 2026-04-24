ALTER TABLE notifications
    ADD COLUMN event_occurred_at TIMESTAMPTZ;

UPDATE notifications
SET event_occurred_at = processed_at
WHERE event_occurred_at IS NULL;

ALTER TABLE notifications
    ALTER COLUMN event_occurred_at SET NOT NULL;

DROP INDEX IF EXISTS ux_notifications_dedup;

CREATE UNIQUE INDEX ux_notifications_dedup
    ON notifications (appointment_id, event_type, recipient_email, event_occurred_at);
