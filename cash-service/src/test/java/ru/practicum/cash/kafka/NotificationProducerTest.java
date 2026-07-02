package ru.practicum.cash.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import ru.practicum.cash.model.NotificationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@EmbeddedKafka(partitions = 1, topics = NotificationProducer.TOPIC)
class NotificationProducerTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void shouldSendNotificationToKafka() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(broker));
        producerProps.put("key.serializer", StringSerializer.class);
        producerProps.put("value.serializer", JsonSerializer.class);
        producerProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        KafkaTemplate<String, Object> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        NotificationProducer producer = new NotificationProducer(template);

        producer.send("user", "Пополнение на сумму 500", NotificationType.DEPOSIT);

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps("test-group", "true", broker));
        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationMessage.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        try (Consumer<String, NotificationMessage> consumer =
                     new DefaultKafkaConsumerFactory<String, NotificationMessage>(consumerProps).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, NotificationProducer.TOPIC);
            ConsumerRecord<String, NotificationMessage> record =
                    KafkaTestUtils.getSingleRecord(consumer, NotificationProducer.TOPIC, Duration.ofSeconds(10));

            assertNotNull(record);
            assertEquals("user", record.key());
            NotificationMessage message = record.value();
            assertEquals("user", message.getLogin());
            assertEquals(NotificationType.DEPOSIT, message.getType());
            assertNotNull(message.getEventId());
        }
    }
}
