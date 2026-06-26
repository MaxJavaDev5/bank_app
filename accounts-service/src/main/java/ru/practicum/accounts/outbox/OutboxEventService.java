package ru.practicum.accounts.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.model.OutboxStatus;
import ru.practicum.accounts.repository.OutboxRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxEventService {

    private static final long MAX_BACKOFF_SECONDS = 3600;

    private final OutboxRepository outboxRepository;
    private final String instanceId = UUID.randomUUID().toString();

    @Value("${outbox.batch-size:10}")
    private int batchSize;

    @Value("${outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${outbox.base-delay-seconds:30}")
    private long baseDelaySeconds;

    @Value("${outbox.lock-timeout-seconds:60}")
    private long lockTimeoutSeconds;

    public OutboxEventService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    // атомарно забирает события для обработки этим инстансом
    @Transactional
    public List<OutboxEvent> claimPending() {
        Instant stuckBefore = Instant.now().minusSeconds(lockTimeoutSeconds);
        outboxRepository.claimEvents(instanceId, batchSize, stuckBefore);
        return outboxRepository.findByStatusAndLockedByOrderByIdAsc(OutboxStatus.PROCESSING, instanceId);
    }

    @Transactional
    public void markProcessed(Long eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxStatus.PROCESSED);
            event.setLockedAt(null);
            event.setLockedBy(null);
            event.setLastError(null);
            outboxRepository.save(event);
        });
    }

    @Transactional
    public void markFailed(Long eventId, String error) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            int newAttempts = event.getAttempts() + 1;
            event.setAttempts(newAttempts);
            event.setLastError(truncateError(error));
            event.setLockedAt(null);
            event.setLockedBy(null);

            if (newAttempts >= maxAttempts) {
                event.setStatus(OutboxStatus.DEAD);
            } else {
                event.setStatus(OutboxStatus.FAILED);
                event.setNextAttemptAt(Instant.now().plusSeconds(calculateBackoff(newAttempts)));
            }
            outboxRepository.save(event);
        });
    }

    private long calculateBackoff(int attempts) {
        long delay = baseDelaySeconds * (1L << (attempts - 1));
        return Math.min(delay, MAX_BACKOFF_SECONDS);
    }

    private String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}
