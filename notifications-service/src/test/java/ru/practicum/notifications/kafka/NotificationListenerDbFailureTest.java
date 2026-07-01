package ru.practicum.notifications.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.model.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(KafkaRawMessageTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
        KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC
})
class NotificationListenerDbFailureTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker broker;

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

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertNotNull(pollDltRecord()));

        assertTrue(notificationRepository.findByEventId(eventId).isEmpty());
    }

    private ConsumerRecord<String, String> pollDltRecord() {
        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps("dlt-db-test-" + UUID.randomUUID(), "true", broker));
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", StringDeserializer.class);

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC);
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                    consumer, Duration.ofMillis(500));
            return records.isEmpty() ? null : records.iterator().next();
        }
    }
}
