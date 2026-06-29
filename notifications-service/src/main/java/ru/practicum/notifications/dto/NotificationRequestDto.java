package ru.practicum.notifications.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import ru.practicum.notifications.model.Notification;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequestDto {

    private String login;

    private String message;

    private Notification.NotificationType type;

    private String eventId;
}
