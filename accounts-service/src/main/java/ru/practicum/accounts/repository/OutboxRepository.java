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
}
