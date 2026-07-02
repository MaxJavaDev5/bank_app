package ru.practicum.cash.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.cash.model.NotificationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    private String eventId;

    private String login;

    private String message;

    private NotificationType type;
}
