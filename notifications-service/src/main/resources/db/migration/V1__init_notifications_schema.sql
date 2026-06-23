CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
    id         BIGSERIAL PRIMARY KEY,
    login      VARCHAR(255) NOT NULL,
    message    VARCHAR(255) NOT NULL,
    type       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
