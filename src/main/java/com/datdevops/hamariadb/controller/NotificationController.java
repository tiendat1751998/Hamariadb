package com.datdevops.hamariadb.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.entity.TelegramNotification;
import com.datdevops.hamariadb.service.notification.NotificationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TelegramNotification>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get notifications for user: {}", username);

        List<TelegramNotification> notifications = notificationService.getUserNotifications(username, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications retrieved successfully"));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Mark notification as read: {} for user: {}", notificationId, username);

        notificationService.markNotificationAsRead(notificationId, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> sendTestNotification(Authentication authentication) {
        String username = authentication.getName();
        log.info("Sending test notification for user: {}", username);

        String mess = notificationService.sendBalanceNotification("admin", "1,000,000 VND", "10,000,000 VND", "Test notification");

        return ResponseEntity.ok(ApiResponse.success(mess, "Test notification sent"));
    }
}
