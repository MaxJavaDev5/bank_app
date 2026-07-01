package ru.practicum.accounts.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.accounts.model.NotificationType;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    // общий топик уведомлений, его слушает notifications-service
    public static final String TOPIC = "bank.notifications";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // шлём уведомление в Kafka, ключ = login чтобы события одного юзера были по порядку
    public void send(String login, String message, NotificationType type) {
        String eventId = UUID.randomUUID().toString();
        NotificationMessage payload = new NotificationMessage(eventId, login, message, type);
        kafkaTemplate.send(TOPIC, login, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Не удалось отправить уведомление в Kafka: login={}, type={}, eventId={}",
                                login, type, eventId, ex);
                    } else {
                        log.info("Уведомление доставлено в Kafka: login={}, type={}, eventId={}, partition={}, offset={}",
                                login, type, eventId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
