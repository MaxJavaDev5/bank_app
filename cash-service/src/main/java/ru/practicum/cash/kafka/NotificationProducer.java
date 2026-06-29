package ru.practicum.cash.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.cash.model.NotificationType;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    // общий топик уведомлений, его слушает notifications-service
    public static final String TOPIC = "bank.notifications";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String login, String message, NotificationType type) {
        String eventId = UUID.randomUUID().toString();
        NotificationMessage payload = new NotificationMessage(eventId, login, message, type);
        kafkaTemplate.send(TOPIC, login, payload);
        log.info("Отправили уведомление в Kafka: login={}, type={}, eventId={}", login, type, eventId);
    }
}
