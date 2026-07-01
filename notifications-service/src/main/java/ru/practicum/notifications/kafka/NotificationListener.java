package ru.practicum.notifications.kafka;

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

    @KafkaListener(
            topics = KafkaTopicsConfig.NOTIFICATIONS_TOPIC,
            containerFactory = "kafkaListenerContainerFactory")
    public void onNotification(@Valid @Payload NotificationRequestDto request) {
        notificationService.createNotification(request);
    }
}
