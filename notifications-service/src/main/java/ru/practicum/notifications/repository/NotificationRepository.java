package ru.practicum.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.notifications.model.Notification;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventId(String eventId);
}
