SET search_path TO accounts;

ALTER TABLE outbox_events ADD COLUMN status VARCHAR(255);
ALTER TABLE outbox_events ADD COLUMN attempts INT NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(1000);
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMP;
ALTER TABLE outbox_events ADD COLUMN locked_at TIMESTAMP;
ALTER TABLE outbox_events ADD COLUMN locked_by VARCHAR(255);

UPDATE outbox_events SET status = 'PROCESSED' WHERE processed = true;
UPDATE outbox_events SET status = 'PENDING', next_attempt_at = now() WHERE processed = false;

ALTER TABLE outbox_events ALTER COLUMN status SET NOT NULL;
ALTER TABLE outbox_events ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE outbox_events DROP COLUMN processed;

CREATE INDEX idx_outbox_events_status_next_attempt ON outbox_events (status, next_attempt_at);
