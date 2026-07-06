package ru.practicum.notifications.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.model.Notification;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerDeserializationTest {

    @Test
    void shouldDeserializePlainJsonWithNarrowTrustedPackages() {
        JsonDeserializer<NotificationRequestDto> deserializer =
                new JsonDeserializer<>(NotificationRequestDto.class, false);
        deserializer.addTrustedPackages(
                "ru.practicum.notifications.dto",
                "ru.practicum.notifications.model");

        String eventId = UUID.randomUUID().toString();
        String json = """
                {
                  "eventId": "%s",
                  "login": "user",
                  "message": "Пополнение на 500",
                  "type": "DEPOSIT"
                }
                """.formatted(eventId);

        NotificationRequestDto result = deserializer.deserialize(
                KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
                json.getBytes(StandardCharsets.UTF_8));

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo(eventId);
        assertThat(result.getLogin()).isEqualTo("user");
        assertThat(result.getMessage()).isEqualTo("Пополнение на 500");
        assertThat(result.getType()).isEqualTo(Notification.NotificationType.DEPOSIT);
    }
}
