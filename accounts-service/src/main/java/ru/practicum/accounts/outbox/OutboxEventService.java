package ru.practicum.accounts.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.repository.OutboxRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchPending() {
        return outboxRepository.findByProcessedFalseOrderByIdAsc(Pageable.ofSize(10));
    }

    @Transactional
    public void markProcessed(Long eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setProcessed(true);
            outboxRepository.save(event);
        });
    }
}
