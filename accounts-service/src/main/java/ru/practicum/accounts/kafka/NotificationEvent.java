package ru.practicum.accounts.kafka;

import ru.practicum.accounts.model.NotificationType;

public record NotificationEvent(String login, String message, NotificationType type) {
}
