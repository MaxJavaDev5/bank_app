ALTER TABLE notifications.notifications
    ADD COLUMN event_id BIGINT;

CREATE UNIQUE INDEX uq_notifications_event_id
    ON notifications.notifications (event_id);
