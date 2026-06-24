package ru.practicum.notifications.mapper;

import org.mapstruct.Mapper;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.model.Notification;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    Notification toNotification(NotificationRequestDto request);

    NotificationDto toNotificationDto(Notification notification);

    List<NotificationDto> toNotificationDtoList(List<Notification> notifications);
}
