package ru.practicum.accounts.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.accounts.model.NotificationType;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.model.OutboxStatus;
import ru.practicum.accounts.repository.OutboxRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxEventService.class)
class OutboxEventServiceTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxEventService outboxEventService;

    private String instanceId;

    @BeforeEach
    void setUp() {
        instanceId = (String) ReflectionTestUtils.getField(outboxEventService, "instanceId");
        ReflectionTestUtils.setField(outboxEventService, "maxAttempts", 5);
        ReflectionTestUtils.setField(outboxEventService, "baseDelaySeconds", 30L);
    }

    @Test
    void shouldMarkProcessedWhenLockedByCurrentInstance() {
        OutboxEvent event = saveProcessingEvent(instanceId);

        outboxEventService.markProcessed(event.getId());

        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSED, updated.getStatus());
        assertNull(updated.getLockedBy());
        assertNull(updated.getLockedAt());
        assertNull(updated.getLastError());
    }

    @Test
    void shouldNotMarkProcessedWhenLockedByAnotherInstance() {
        OutboxEvent event = saveProcessingEvent("other-instance");

        outboxEventService.markProcessed(event.getId());

        OutboxEvent unchanged = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSING, unchanged.getStatus());
        assertEquals("other-instance", unchanged.getLockedBy());
        assertNotNull(unchanged.getLockedAt());
    }

    @Test
    void shouldNotMarkFailedWhenLockedByAnotherInstance() {
        OutboxEvent event = saveProcessingEvent("other-instance");
        event.setAttempts(2);
        outboxRepository.save(event);

        outboxEventService.markFailed(event.getId(), "stale handler error");

        OutboxEvent unchanged = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSING, unchanged.getStatus());
        assertEquals("other-instance", unchanged.getLockedBy());
        assertEquals(2, unchanged.getAttempts());
        assertNull(unchanged.getLastError());
    }

    @Test
    void shouldNotMarkFailedWhenEventAlreadyProcessed() {
        OutboxEvent event = saveProcessingEvent(instanceId);
        event.setStatus(OutboxStatus.PROCESSED);
        event.setLockedBy(null);
        event.setLockedAt(null);
        outboxRepository.save(event);

        outboxEventService.markFailed(event.getId(), "late failure");

        OutboxEvent unchanged = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSED, unchanged.getStatus());
        assertNull(unchanged.getLastError());
    }

    @Test
    void shouldMarkFailedWhenLockedByCurrentInstance() {
        OutboxEvent event = saveProcessingEvent(instanceId);
        event.setAttempts(1);
        outboxRepository.save(event);

        outboxEventService.markFailed(event.getId(), "send failed");

        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, updated.getStatus());
        assertEquals(2, updated.getAttempts());
        assertEquals("send failed", updated.getLastError());
        assertNull(updated.getLockedBy());
        assertNull(updated.getLockedAt());
        assertNotNull(updated.getNextAttemptAt());
    }

    private OutboxEvent saveProcessingEvent(String lockedBy) {
        OutboxEvent event = new OutboxEvent();
        event.setLogin("user");
        event.setMessage("test message");
        event.setEventType(NotificationType.DEPOSIT);
        event.setStatus(OutboxStatus.PROCESSING);
        event.setLockedBy(lockedBy);
        event.setLockedAt(Instant.now());
        return outboxRepository.save(event);
    }
}
