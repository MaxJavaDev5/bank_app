package ru.practicum.notifications.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class KafkaDltTestSupport {

    private static final String DLT_TEST_GROUP = "dlt-test-group";

    @Autowired
    protected EmbeddedKafkaBroker broker;

    private Consumer<String, byte[]> dltConsumer;

    @BeforeEach
    void setUpDltConsumer() {
        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps(DLT_TEST_GROUP + "-" + UUID.randomUUID(), "true", broker));
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", ByteArrayDeserializer.class);

        dltConsumer = new DefaultKafkaConsumerFactory<String, byte[]>(consumerProps).createConsumer();
        broker.consumeFromAnEmbeddedTopic(dltConsumer, KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC);

        var deadline = System.currentTimeMillis() + 5_000;
        while (dltConsumer.assignment().isEmpty() && System.currentTimeMillis() < deadline) {
            dltConsumer.poll(Duration.ofMillis(100));
        }
        var partitions = dltConsumer.assignment();
        if (!partitions.isEmpty()) {
            dltConsumer.seekToEnd(partitions);
        }
    }

    @AfterEach
    void tearDownDltConsumer() {
        if (dltConsumer != null) {
            dltConsumer.close();
        }
    }

    protected ConsumerRecord<String, byte[]> awaitDltRecord(Duration timeout) {
        return KafkaTestUtils.getSingleRecord(
                dltConsumer, KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC, timeout);
    }

    protected void assertDltRecord(
            ConsumerRecord<String, byte[]> record, String expectedKey, String payloadContains) {
        assertEquals(KafkaTopicsConfig.NOTIFICATIONS_DLT_TOPIC, record.topic());
        assertEquals(expectedKey, record.key());
        assertNotNull(record.value());

        String payload = dltPayloadText(record);
        assertTrue(
                payload.contains(payloadContains),
                () -> "Expected payload to contain '%s', but was: %s".formatted(payloadContains, payload));

        var originalTopicHeader = record.headers().lastHeader("kafka_dlt-original-topic");
        assertNotNull(originalTopicHeader);
        assertEquals(
                KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
                new String(originalTopicHeader.value(), StandardCharsets.UTF_8));

        assertNotNull(record.headers().lastHeader("kafka_dlt-original-partition"));
        assertNotNull(record.headers().lastHeader("kafka_dlt-exception-fqcn"));

        var exceptionMessageHeader = record.headers().lastHeader("kafka_dlt-exception-message");
        assertNotNull(exceptionMessageHeader);
        String exceptionMessage = new String(exceptionMessageHeader.value(), StandardCharsets.UTF_8);
        assertTrue(
                !exceptionMessage.isBlank(),
                () -> "kafka_dlt-exception-message should not be blank, but was: '%s'".formatted(exceptionMessage));
    }

    // JsonSerializer пишет byte[] в DLT как JSON-строку с base64 (deserialization errors).
    private String dltPayloadText(ConsumerRecord<String, byte[]> record) {
        String raw = new String(record.value(), StandardCharsets.UTF_8);
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            String inner = raw.substring(1, raw.length() - 1);
            try {
                return new String(Base64.getDecoder().decode(inner), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                return inner;
            }
        }
        return raw;
    }
}
