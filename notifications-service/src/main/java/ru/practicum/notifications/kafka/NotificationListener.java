package ru.practicum.notifications.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.service.NotificationService;

@Validated
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
            containerFactory = "kafkaListenerContainerFactory")
    public void onNotification(@Valid @Payload NotificationRequestDto request) {
        try {
            notificationService.createNotification(request);
        } catch (Exception ex) {
            meterRegistry.counter("bank_notification_failed_total",
                    "login", request.getLogin()).increment();
            throw ex;
        }
    }
}
