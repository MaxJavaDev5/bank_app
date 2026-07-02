package ru.practicum.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.notifications.model.Notification;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequestDto {

    @NotBlank
    private String login;

    @NotBlank
    private String message;

    @NotNull
    private Notification.NotificationType type;

    @NotBlank
    private String eventId;
}
