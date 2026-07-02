package ru.practicum.notifications.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(KafkaRawMessageTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
        KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC
})
class NotificationListenerDbFailureTest extends KafkaDltTestSupport {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void shouldSendToDltWhenDbSaveFails() {
        String eventId = UUID.randomUUID().toString();
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(eventId);
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        when(notificationService.createNotification(any())).thenThrow(new RuntimeException("db down"));

        kafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, request.getLogin(), request);

        assertDltRecord(awaitDltRecord(Duration.ofSeconds(25)), "user", eventId);
        assertTrue(notificationRepository.findByEventId(eventId).isEmpty());
    }
}
