package ru.practicum.notifications.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.practicum.notifications.dto.NotificationRequestDto;
import ru.practicum.notifications.dto.NotificationDto;
import ru.practicum.notifications.security.JwtUtils;
import ru.practicum.notifications.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody NotificationRequestDto request) {
        NotificationService.NotificationCreationResult result =
                notificationService.createNotification(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.notification());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public List<NotificationDto> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getNotificationsByLogin(JwtUtils.getLogin(jwt));
    }
}
