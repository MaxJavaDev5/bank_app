package ru.practicum.notifications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;

import java.util.List;

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
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setLogin("user");

        NotificationDto expectedDto = new NotificationDto();
        expectedDto.setId(1L);
        expectedDto.setLogin("user");

        when(notificationMapper.toNotification(request)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toNotificationDto(notification)).thenReturn(expectedDto);

        NotificationDto result = notificationService.createNotification(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(notificationRepository, times(1)).save(notification);
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
