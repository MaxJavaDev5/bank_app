package ru.practicum.accounts.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.accounts.model.NotificationType;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private final WebClient notificationsWebClient;

    public void sendNotification(Long eventId, String login, String message, NotificationType type) {
        notificationsWebClient.post()
                .uri("/notifications")
                .bodyValue(Map.of(
                        "eventId", eventId,
                        "login", login,
                        "message", message,
                        "type", type
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
