package ru.practicum.notifications.mapper;

import org.mapstruct.Mapper;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.model.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    Notification toNotification(NotificationRequestDto request);
}
