package ru.practicum.notifications.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopicsConfig.NOTIFICATIONS_TOPIC)
    public void onNotification(NotificationRequestDto request) {
        notificationService.createNotification(request);
    }
}
