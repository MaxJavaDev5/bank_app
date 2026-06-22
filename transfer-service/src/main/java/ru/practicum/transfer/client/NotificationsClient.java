package ru.practicum.transfer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.transfer.model.NotificationType;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);

    private final WebClient notificationsWebClient;

    public NotificationsClient(WebClient notificationsWebClient) {
        this.notificationsWebClient = notificationsWebClient;
    }

    public void notifySender(String fromLogin, String toLogin, BigDecimal amount) {
        String message = "Вы перевели " + amount + " руб. пользователю " + toLogin;
        sendNotification(fromLogin, message, NotificationType.TRANSFER_OUT);
    }

    public void notifyReceiver(String toLogin, String fromLogin, BigDecimal amount) {
        String message = "Вы получили " + amount + " рублей от " + fromLogin;
        sendNotification(toLogin, message, NotificationType.TRANSFER_IN);
    }

    private void sendNotification(String login, String message, NotificationType type) {
        try {
            notificationsWebClient.post()
                    .uri("/notifications")
                    .bodyValue(Map.of(
                            "login", login,
                            "message", message,
                            "type", type
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            log.error("Не удалось отправить уведомление для {}: {}", login, ex.getMessage());
        }
    }
}
