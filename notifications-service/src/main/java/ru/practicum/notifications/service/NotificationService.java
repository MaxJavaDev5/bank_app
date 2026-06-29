package ru.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationCreationResult createNotification(NotificationRequestDto request) {
        Optional<Notification> existing = notificationRepository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            return new NotificationCreationResult(
                    notificationMapper.toNotificationDto(existing.get()), false);
        }

        try {
            Notification notification = notificationMapper.toNotification(request);
            Notification saved = notificationRepository.save(notification);
            log.info("уведомление для {}: {}", request.getLogin(), request.getMessage());
            return new NotificationCreationResult(
                    notificationMapper.toNotificationDto(saved), true);
        } catch (DataIntegrityViolationException ex) {
            // параллельный поток уже сохранил это событие — перечитываем и отдаём существующее
            Notification saved = notificationRepository.findByEventId(request.getEventId())
                    .orElseThrow(() -> ex);
            return new NotificationCreationResult(
                    notificationMapper.toNotificationDto(saved), false);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByLogin(String login) {
        List<Notification> notifications = notificationRepository.findByLoginOrderByCreatedAtDesc(login);
        return notificationMapper.toNotificationDtoList(notifications);
    }

    public record NotificationCreationResult(NotificationDto notification, boolean created) {
    }
}
