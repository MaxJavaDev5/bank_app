package ru.practicum.notifications.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = KafkaTopicsConfig.NOTIFICATIONS_TOPIC)
class NotificationListenerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldConsumeMessageAndSaveNotification() {
        String eventId = UUID.randomUUID().toString();
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(eventId);
        request.setLogin("user");
        request.setMessage("Пополнение на 500 рублей");
        request.setType(Notification.NotificationType.DEPOSIT);

        kafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, request.getLogin(), request);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertTrue(notificationRepository.findByEventId(eventId).isPresent()));

        Notification saved = notificationRepository.findByEventId(eventId).orElseThrow();
        assertEquals("user", saved.getLogin());
        assertEquals(Notification.NotificationType.DEPOSIT, saved.getType());
    }

    @Test
    void shouldSkipDuplicateEvents() {
        String eventId = UUID.randomUUID().toString();
        NotificationRequestDto request = new NotificationRequestDto();
        request.setEventId(eventId);
        request.setLogin("dupe");
        request.setMessage("Списание на 100 рублей");
        request.setType(Notification.NotificationType.WITHDRAW);

        kafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, request.getLogin(), request);
        kafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, request.getLogin(), request);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertTrue(notificationRepository.findByEventId(eventId).isPresent()));

        assertEquals(1, notificationRepository.findAll().stream()
                .filter(n -> eventId.equals(n.getEventId()))
                .count());
    }
}
