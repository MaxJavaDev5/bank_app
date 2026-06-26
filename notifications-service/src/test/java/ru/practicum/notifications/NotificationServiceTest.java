package ru.practicum.notifications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;

import java.util.List;
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

    @Test
    void shouldCreateNotification() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(1L);
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setLogin("user");

        NotificationDto expectedDto = new NotificationDto();
        expectedDto.setId(1L);
        expectedDto.setLogin("user");

        when(notificationRepository.findByEventId(1L)).thenReturn(Optional.empty());
        when(notificationMapper.toNotification(request)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toNotificationDto(notification)).thenReturn(expectedDto);

        NotificationService.NotificationCreationResult result =
                notificationService.createNotification(request);

        assertNotNull(result.notification());
        assertEquals(1L, result.notification().getId());
        assertTrue(result.created());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void shouldReturnExistingNotificationWhenEventIdAlreadyExists() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(1L);
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        Notification existing = new Notification();
        existing.setId(1L);
        existing.setEventId(1L);

        NotificationDto expectedDto = new NotificationDto();
        expectedDto.setId(1L);
        expectedDto.setEventId(1L);

        when(notificationRepository.findByEventId(1L)).thenReturn(Optional.of(existing));
        when(notificationMapper.toNotificationDto(existing)).thenReturn(expectedDto);

        NotificationService.NotificationCreationResult result =
                notificationService.createNotification(request);

        assertEquals(1L, result.notification().getId());
        assertFalse(result.created());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldReturnExistingNotificationWhenSaveFailsDueToRaceCondition() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(1L);
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        Notification newNotification = new Notification();
        newNotification.setLogin("user");

        Notification existing = new Notification();
        existing.setId(1L);
        existing.setEventId(1L);

        NotificationDto expectedDto = new NotificationDto();
        expectedDto.setId(1L);
        expectedDto.setEventId(1L);

        when(notificationRepository.findByEventId(1L))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(notificationMapper.toNotification(request)).thenReturn(newNotification);
        when(notificationRepository.save(newNotification))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(notificationMapper.toNotificationDto(existing)).thenReturn(expectedDto);

        NotificationService.NotificationCreationResult result =
                notificationService.createNotification(request);

        assertEquals(1L, result.notification().getId());
        assertFalse(result.created());
        verify(notificationRepository, times(2)).findByEventId(1L);
        verify(notificationRepository, times(1)).save(newNotification);
    }

    @Test
    void shouldReturnNotificationsForUser() {
        Notification notification = new Notification();
        notification.setLogin("user");

        NotificationDto dto = new NotificationDto();
        dto.setLogin("user");

        when(notificationRepository.findByLoginOrderByCreatedAtDesc("user"))
                .thenReturn(List.of(notification));
        when(notificationMapper.toNotificationDtoList(List.of(notification)))
                .thenReturn(List.of(dto));

        List<NotificationDto> result = notificationService.getNotificationsByLogin("user");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository, times(1))
                .findByLoginOrderByCreatedAtDesc("user");
    }

    @Test
    void shouldReturnEmptyListWhenNoNotifications() {
        when(notificationRepository.findByLoginOrderByCreatedAtDesc("user"))
                .thenReturn(List.of());
        when(notificationMapper.toNotificationDtoList(List.of()))
                .thenReturn(List.of());

        List<NotificationDto> result = notificationService.getNotificationsByLogin("user");

        assertTrue(result.isEmpty());
    }
}
