package ru.practicum.accounts.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.accounts.client.NotificationsClient;
import ru.practicum.accounts.model.NotificationType;
import ru.practicum.accounts.model.OutboxEvent;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void shouldMarkProcessedAfterSuccessfulNotification() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setLogin("user");
        event.setMessage("test");
        event.setEventType(NotificationType.DEPOSIT);

        when(outboxEventService.claimPending()).thenReturn(List.of(event));

        outboxProcessor.process();

        verify(notificationsClient).sendNotification(1L, "user", "test", NotificationType.DEPOSIT);
        verify(outboxEventService).markProcessed(1L);
        verify(outboxEventService, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void shouldMarkFailedWhenNotificationFails() {
        OutboxEvent event = new OutboxEvent();
        event.setId(2L);
        event.setLogin("user");
        event.setMessage("fail");
        event.setEventType(NotificationType.WITHDRAW);

        when(outboxEventService.claimPending()).thenReturn(List.of(event));
        doThrow(new RuntimeException("notifications down"))
                .when(notificationsClient)
                .sendNotification(2L, "user", "fail", NotificationType.WITHDRAW);

        outboxProcessor.process();

        verify(outboxEventService).markFailed(2L, "notifications down");
        verify(outboxEventService, never()).markProcessed(anyLong());
    }
}
