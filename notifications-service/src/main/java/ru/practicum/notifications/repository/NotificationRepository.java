package ru.practicum.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.notifications.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByLoginOrderByCreatedAtDesc(String login);
    Optional<Notification> findByEventId(Long eventId);
}
