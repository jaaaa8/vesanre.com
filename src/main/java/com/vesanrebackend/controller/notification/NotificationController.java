package com.vesanrebackend.controller.notification;

import com.vesanrebackend.dto.notification.NotificationPage;
import com.vesanrebackend.service.notification.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public NotificationPage list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "false") boolean unreadOnly,
                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notifications.list(UUID.fromString(jwt.getSubject()), unreadOnly, page, size);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        notifications.markRead(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
