package ru.practicum.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import ru.practicum.notifications.model.Notification;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequestDto {

    @NotBlank(message = "Логин не может быть пустым")
    private String login;

    @NotBlank(message = "Сообщение не может быть пустым")
    private String message;

    @NotNull(message = "Тип уведомления не может быть null")
    private Notification.NotificationType type;
}
