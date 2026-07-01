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
import ru.practicum.notifications.repository.NotificationRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
@SpringBootTest
@Import(KafkaRawMessageTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
        KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC
})
class NotificationListenerFailureTest {

    @Autowired
    private KafkaTemplate<String, String> rawKafkaTemplate;
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldSendBrokenJsonToDlt() {
        rawKafkaTemplate.send(KafkaTopicsConfig.NOTIFICATIONS_TOPIC, "user", "{broken json");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertNotNull(pollDltRecord()));
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

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertNotNull(pollDltRecord()));
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

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertNotNull(pollDltRecord()));

        assertTrue(notificationRepository.findAll().isEmpty());
    }

    private ConsumerRecord<String, String> pollDltRecord() {
        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps("dlt-test-" + UUID.randomUUID(), "true", broker));
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
