package ru.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.accounts.model.OutboxEvent;
import ru.practicum.accounts.model.OutboxStatus;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndLockedByOrderByIdAsc(OutboxStatus status, String lockedBy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE accounts.outbox_events
            SET status = 'PROCESSING', locked_by = :instanceId, locked_at = now()
            WHERE id IN (
                SELECT id FROM accounts.outbox_events
                WHERE (status IN ('PENDING', 'FAILED') AND next_attempt_at <= now())
                   OR (status = 'PROCESSING' AND locked_at < :stuckBefore)
                ORDER BY id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            """, nativeQuery = true)
    int claimEvents(@Param("instanceId") String instanceId,
                    @Param("limit") int limit,
                    @Param("stuckBefore") Instant stuckBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = ru.practicum.accounts.model.OutboxStatus.PROCESSED,
                e.lockedAt = null, e.lockedBy = null, e.lastError = null
            WHERE e.id = :id
              AND e.status = ru.practicum.accounts.model.OutboxStatus.PROCESSING
              AND e.lockedBy = :instanceId
            """)
    int markProcessed(@Param("id") Long id, @Param("instanceId") String instanceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = :status,
                e.attempts = :attempts,
                e.lastError = :lastError,
                e.nextAttemptAt = :nextAttemptAt,
                e.lockedAt = null,
                e.lockedBy = null
            WHERE e.id = :id
              AND e.status = ru.practicum.accounts.model.OutboxStatus.PROCESSING
              AND e.lockedBy = :instanceId
            """)
    int markFailed(@Param("id") Long id,
                   @Param("instanceId") String instanceId,
                   @Param("status") OutboxStatus status,
                   @Param("attempts") int attempts,
                   @Param("lastError") String lastError,
                   @Param("nextAttemptAt") Instant nextAttemptAt);
}
