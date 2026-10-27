package com.org.pfe.notification_service.controller;

import com.org.pfe.notification_service.dto.FraudAlertRequest;
import com.org.pfe.notification_service.dto.NotificationResponse;
import com.org.pfe.notification_service.security.NotificationPrincipal;
import com.org.pfe.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${notification.api-key:}")
    private String apiKey;

    @PostMapping("/fraud-alert")
    public ResponseEntity<Void> receiveFraudAlert(
            @Valid @RequestBody FraudAlertRequest request,
            @RequestHeader(value = "X-Notification-Api-Key", required = false) String providedApiKey
    ) {
        if (apiKey.isBlank() || providedApiKey == null || !MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                providedApiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        notificationService.handleFraudAlert(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mine")
    public List<NotificationResponse> getMine(Principal principal) {
        return notificationService.getForUser(userId(principal));
    }

    @GetMapping("/mine/unread-count")
    public Map<String, Long> getUnreadCount(Principal principal) {
        return Map.of("count", notificationService.getUnreadCount(userId(principal)));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id, Principal principal) {
        return notificationService.markAsRead(userId(principal), id);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/retry-email")
    public NotificationResponse retryEmail(@PathVariable Long id, Principal principal) {
        return notificationService.retryEmail(userId(principal), id);
    }

    private Long userId(Principal principal) {
        return ((NotificationPrincipal) principal).userId();
    }
}