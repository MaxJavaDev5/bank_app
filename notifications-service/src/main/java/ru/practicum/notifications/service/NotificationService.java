package ru.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationCreationResult createNotification(NotificationRequestDto request) {
        return notificationRepository.findByEventId(request.getEventId())
                .map(notification -> new NotificationCreationResult(
                        notificationMapper.toNotificationDto(notification), false))
                .orElseGet(() -> {
                    Notification notification = notificationMapper.toNotification(request);
                    Notification savedNotification = notificationRepository.save(notification);
                    log.info("уведомление для {}: {}", request.getLogin(), request.getMessage());
                    return new NotificationCreationResult(
                            notificationMapper.toNotificationDto(savedNotification), true);
                });
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByLogin(String login) {
        List<Notification> notifications = notificationRepository.findByLoginOrderByCreatedAtDesc(login);
        return notificationMapper.toNotificationDtoList(notifications);
    }

    public record NotificationCreationResult(NotificationDto notification, boolean created) {
    }
}
