CREATE SCHEMA IF NOT EXISTS accounts;

CREATE TABLE accounts.accounts (
    id         BIGSERIAL PRIMARY KEY,
    login      VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    birth_date DATE,
    balance    DECIMAL(19, 2) NOT NULL DEFAULT 0,
    version    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE accounts.outbox_events (
    id         BIGSERIAL PRIMARY KEY,
    login      VARCHAR(255) NOT NULL,
    message    VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    processed  BOOLEAN NOT NULL DEFAULT FALSE
);
