package ru.practicum.accounts.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.accounts.model.NotificationType;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void shouldSendNotificationAfterCommit() {
        NotificationEvent event = new NotificationEvent(
                "user", "Ваш профиль успешно обновлён", NotificationType.PROFILE_UPDATE);

        listener.onNotification(event);

        verify(notificationProducer).send("user", "Ваш профиль успешно обновлён", NotificationType.PROFILE_UPDATE);
    }
}
