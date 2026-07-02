package ru.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public boolean createNotification(NotificationRequestDto request) {
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required for deduplication");
        }

        if (notificationRepository.findByEventId(request.getEventId()).isPresent()) {
            return false;
        }

        try {
            Notification notification = notificationMapper.toNotification(request);
            notificationRepository.save(notification);
            log.info("уведомление для {}: {}", request.getLogin(), request.getMessage());
            return true;
        } catch (DataIntegrityViolationException ex) {
            // параллельно это событие уже сохранили — считаем дублем
            return false;
        }
    }
}
