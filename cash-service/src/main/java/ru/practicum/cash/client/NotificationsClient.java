package ru.practicum.cash.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.cash.model.NotificationType;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);

    private final WebClient notificationsWebClient;

    public NotificationsClient(WebClient notificationsWebClient) {
        this.notificationsWebClient = notificationsWebClient;
    }

    public void notifyDeposit(String login, BigDecimal amount) {
        String message = "Ваш счёт пополнен на " + amount + " рублей";
        sendNotification(login, message, NotificationType.DEPOSIT);
    }

    public void notifyWithdraw(String login, BigDecimal amount) {
        String message = "Со счёта снято " + amount + " рублей";
        sendNotification(login, message, NotificationType.WITHDRAW);
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
