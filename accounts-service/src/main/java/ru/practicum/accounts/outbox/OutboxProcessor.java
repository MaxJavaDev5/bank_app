package ru.practicum.accounts.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.client.NotificationsClient;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.repository.OutboxRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final NotificationsClient notificationsClient;

    // раз в 5 сек обрабатываем необработанные ивенты из outbox
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void process() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByIdAsc(Pageable.ofSize(10));

        for (OutboxEvent event : events) {
            try {
                notificationsClient.sendNotification(
                        event.getLogin(),
                        event.getMessage(),
                        event.getEventType()
                );
                event.setProcessed(true);
                outboxRepository.save(event);
            } catch (Exception ex) {
                log.error("Не удалось отправить outbox-событие {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
