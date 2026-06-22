package ru.practicum.notifications.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import ru.practicum.notifications.model.Notification;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private String login;
    private String message;
    private Notification.NotificationType type;
    private LocalDateTime createdAt;
}
