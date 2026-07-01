package ru.practicum.transfer.kafka;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.practicum.transfer.model.NotificationType;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProducerFailureTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private NotificationProducer producer;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void startCapturingLogs() {
        producer = new NotificationProducer(kafkaTemplate);
        Logger logger = (Logger) LoggerFactory.getLogger(NotificationProducer.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void stopCapturingLogs() {
        ((Logger) LoggerFactory.getLogger(NotificationProducer.class)).detachAppender(appender);
    }

    @Test
    void shouldLogErrorWhenBrokerUnavailable() {
        stubFailedSend(new TimeoutException("broker unavailable"));

        producer.send("user", "Перевод 300", NotificationType.TRANSFER_OUT);

        awaitErrorLog();
        assertFalse(hasSuccessLog());
    }

    @Test
    void shouldLogErrorWhenSendFails() {
        stubFailedSend(new org.apache.kafka.common.KafkaException("send failed"));

        producer.send("user", "Перевод 300", NotificationType.TRANSFER_OUT);

        awaitErrorLog();
        assertFalse(hasSuccessLog());
    }

    @Test
    void shouldLogErrorWhenTopicNotFound() {
        stubFailedSend(new UnknownTopicOrPartitionException("topic not found"));

        producer.send("user", "Перевод 300", NotificationType.TRANSFER_OUT);

        awaitErrorLog();
        assertFalse(hasSuccessLog());
    }

    @Test
    void shouldLogErrorWhenSerializationFails() {
        stubFailedSend(new SerializationException("serialization error"));

        producer.send("user", "Перевод 300", NotificationType.TRANSFER_OUT);

        awaitErrorLog();
        assertFalse(hasSuccessLog());
    }

    private void stubFailedSend(Exception exception) {
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(exception);
        when(kafkaTemplate.send(eq(NotificationProducer.TOPIC), eq("user"), any()))
                .thenReturn(failed);
    }

    private void awaitErrorLog() {
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertTrue(hasErrorLog(), "Expected error log from Kafka send callback"));
    }

    private boolean hasErrorLog() {
        return appender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("Не удалось отправить уведомление в Kafka"));
    }

    private boolean hasSuccessLog() {
        return appender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.INFO
                        && event.getFormattedMessage().contains("Уведомление доставлено в Kafka"));
    }
}
