package ru.practicum.notifications.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SERVICE')")
    public NotificationDto createNotification(@Valid @RequestBody NotificationRequestDto request) {
        return notificationService.createNotification(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public List<NotificationDto> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getNotificationsByLogin(JwtUtils.getLogin(jwt));
    }
}
