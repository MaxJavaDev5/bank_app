package ru.practicum.notifications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequestDto request() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId("evt-1");
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);
        return request;
    }

    @Test
    void shouldCreateNotification() {
        NotificationRequestDto request = request();
        Notification notification = new Notification();
        notification.setLogin("user");

        when(notificationRepository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(notificationMapper.toNotification(request)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        boolean created = notificationService.createNotification(request);

        assertTrue(created);
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void shouldSkipWhenEventIdAlreadyExists() {
        NotificationRequestDto request = request();
        Notification existing = new Notification();
        existing.setEventId("evt-1");

        when(notificationRepository.findByEventId("evt-1")).thenReturn(Optional.of(existing));

        boolean created = notificationService.createNotification(request);

        assertFalse(created);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldSkipWhenSaveFailsDueToRaceCondition() {
        NotificationRequestDto request = request();
        Notification newNotification = new Notification();
        newNotification.setLogin("user");

        when(notificationRepository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(notificationMapper.toNotification(request)).thenReturn(newNotification);
        when(notificationRepository.save(newNotification))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        boolean created = notificationService.createNotification(request);

        assertFalse(created);
        verify(notificationRepository, times(1)).save(newNotification);
    }
}
