DELETE FROM notifications.notifications
WHERE event_id IS NULL;

ALTER TABLE notifications.notifications
    ALTER COLUMN event_id SET NOT NULL;
