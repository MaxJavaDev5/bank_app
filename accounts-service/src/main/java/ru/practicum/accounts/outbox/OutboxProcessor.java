package ru.practicum.accounts.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.practicum.accounts.client.NotificationsClient;
import ru.practicum.accounts.model.OutboxEvent;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventService outboxEventService;
    private final NotificationsClient notificationsClient;

    // раз в 5 сек обрабатываем необработанные ивенты из outbox
    @Scheduled(fixedDelay = 5000)
    public void process() {
        List<OutboxEvent> events = outboxEventService.claimPending();

        for (OutboxEvent event : events) {
            try {
                notificationsClient.sendNotification(
                        event.getId(),
                        event.getLogin(),
                        event.getMessage(),
                        event.getEventType()
                );
                outboxEventService.markProcessed(event.getId());
            } catch (Exception ex) {
                log.error("Не удалось отправить outbox-событие {}: {}", event.getId(), ex.getMessage());
                outboxEventService.markFailed(event.getId(), ex.getMessage());
            }
        }
    }
}
