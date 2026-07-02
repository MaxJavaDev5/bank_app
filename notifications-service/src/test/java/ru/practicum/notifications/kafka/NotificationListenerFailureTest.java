package ru.practicum.notifications.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import ru.practicum.notifications.repository.NotificationRepository;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(KafkaRawMessageTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
        KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC
})
class NotificationListenerFailureTest extends KafkaDltTestSupport {

    @Autowired
    private KafkaTemplate<String, String> rawKafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldSendBrokenJsonToDlt() {
        rawKafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, "user", "{broken json");

        assertDltRecord(awaitDltRecord(Duration.ofSeconds(25)), "user", "broken json");
    }

    @Test
    void shouldSendUnknownEnumToDlt() {
        String payload = """
                {
                  "eventId": "%s",
                  "login": "user",
                  "message": "test",
                  "type": "UNKNOWN_TYPE"
                }
                """.formatted(UUID.randomUUID());

        rawKafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, "user", payload);

        assertDltRecord(awaitDltRecord(Duration.ofSeconds(25)), "user", "UNKNOWN_TYPE");
    }

    @Test
    void shouldSendBlankEventIdToDlt() {
        String payload = """
                {
                  "eventId": "",
                  "login": "user",
                  "message": "test",
                  "type": "DEPOSIT"
                }
                """;

        rawKafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, "user", payload);

        assertDltRecord(awaitDltRecord(Duration.ofSeconds(15)), "user", "eventId");
        assertTrue(notificationRepository.findAll().isEmpty());
    }
}
