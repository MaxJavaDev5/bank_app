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
import java.util.List;

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
        ReflectionTestUtils.setField(outboxEventService, "batchSize", 10);
        ReflectionTestUtils.setField(outboxEventService, "lockTimeoutSeconds", 60L);
    }

    @Test
    void shouldClaimDuePendingEvent() {
        OutboxEvent event = saveEvent(OutboxStatus.PENDING, Instant.now().minusSeconds(10), null, null);

        List<OutboxEvent> claimed = outboxEventService.claimPending();

        assertEquals(1, claimed.size());
        assertEquals(event.getId(), claimed.getFirst().getId());
        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSING, updated.getStatus());
        assertEquals(instanceId, updated.getLockedBy());
        assertNotNull(updated.getLockedAt());
    }

    @Test
    void shouldNotClaimNotYetDueFailedEvent() {
        OutboxEvent event = saveEvent(OutboxStatus.FAILED, Instant.now().plusSeconds(300), null, null);

        List<OutboxEvent> claimed = outboxEventService.claimPending();

        assertTrue(claimed.isEmpty());
        OutboxEvent unchanged = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, unchanged.getStatus());
        assertNull(unchanged.getLockedBy());
    }

    @Test
    void shouldNotClaimProcessedOrDeadEvents() {
        saveEvent(OutboxStatus.PROCESSED, Instant.now().minusSeconds(10), null, null);
        saveEvent(OutboxStatus.DEAD, Instant.now().minusSeconds(10), null, null);

        List<OutboxEvent> claimed = outboxEventService.claimPending();

        assertTrue(claimed.isEmpty());
    }

    @Test
    void shouldReclaimStuckProcessingEventAfterTimeout() {
        OutboxEvent event = saveEvent(
                OutboxStatus.PROCESSING,
                null,
                "other-instance",
                Instant.now().minusSeconds(120));

        List<OutboxEvent> claimed = outboxEventService.claimPending();

        assertEquals(1, claimed.size());
        assertEquals(event.getId(), claimed.getFirst().getId());
        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSING, updated.getStatus());
        assertEquals(instanceId, updated.getLockedBy());
        assertNotNull(updated.getLockedAt());
    }

    @Test
    void shouldNotReclaimFreshlyLockedProcessingEvent() {
        OutboxEvent event = saveEvent(
                OutboxStatus.PROCESSING,
                null,
                "other-instance",
                Instant.now().minusSeconds(5));

        List<OutboxEvent> claimed = outboxEventService.claimPending();

        assertTrue(claimed.isEmpty());
        OutboxEvent unchanged = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSING, unchanged.getStatus());
        assertEquals("other-instance", unchanged.getLockedBy());
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

    @Test
    void shouldMarkDeadWhenMaxAttemptsReached() {
        OutboxEvent event = saveProcessingEvent(instanceId);
        event.setAttempts(4);
        outboxRepository.save(event);

        outboxEventService.markFailed(event.getId(), "final failure");

        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.DEAD, updated.getStatus());
        assertEquals(5, updated.getAttempts());
        assertEquals("final failure", updated.getLastError());
        assertNull(updated.getNextAttemptAt());
        assertNull(updated.getLockedBy());
        assertNull(updated.getLockedAt());
    }

    private OutboxEvent saveProcessingEvent(String lockedBy) {
        return saveEvent(OutboxStatus.PROCESSING, null, lockedBy, Instant.now());
    }

    private OutboxEvent saveEvent(OutboxStatus status,
                                  Instant nextAttemptAt,
                                  String lockedBy,
                                  Instant lockedAt) {
        OutboxEvent event = new OutboxEvent();
        event.setLogin("user");
        event.setMessage("test message");
        event.setEventType(NotificationType.DEPOSIT);
        event.setStatus(status);
        event.setNextAttemptAt(nextAttemptAt);
        event.setLockedBy(lockedBy);
        event.setLockedAt(lockedAt);
        return outboxRepository.save(event);
    }
}
