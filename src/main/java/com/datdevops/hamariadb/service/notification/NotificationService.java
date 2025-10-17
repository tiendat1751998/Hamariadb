package com.datdevops.hamariadb.service.notification;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.datdevops.hamariadb.entity.NotificationStatus;
import com.datdevops.hamariadb.entity.TelegramMessageType;
import com.datdevops.hamariadb.entity.TelegramNotification;
import com.datdevops.hamariadb.repository.dao.TelegramNotificationRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {

    private final TelegramNotificationRepository telegramNotificationRepository;
    private final TelegramService telegramService;

    public NotificationService(TelegramNotificationRepository telegramNotificationRepository,
                               TelegramService telegramService) {
        this.telegramNotificationRepository = telegramNotificationRepository;
        this.telegramService = telegramService;
    }

    public String sendBalanceNotification(String accountNumber, String amount, String balance, String description) {
        String message = String.format(
                "💰 Biến động số dư\nTài khoản: %s\nSố tiền: %s\nSố dư: %s\nNội dung: %s",
                accountNumber, amount, balance, description
        );

         saveAndSendNotification("SYSTEM", "BALANCE_UPDATE", message);
         return message;
    }

    public void sendTransactionSuccessNotification(String accountNumber, String amount, String description) {
        String message = String.format(
                "✅ Giao dịch thành công\nTài khoản: %s\nSố tiền: %s\nNội dung: %s",
                accountNumber, amount, description
        );

        saveAndSendNotification("SYSTEM", "TRANSACTION_SUCCESS", message);
    }

    public void sendTransactionFailedNotification(String accountNumber, String amount, String error, String description) {
        String message = String.format(
                "❌ Giao dịch thất bại\nTài khoản: %s\nSố tiền: %s\nLỗi: %s\nNội dung: %s",
                accountNumber, amount, error, description
        );

        saveAndSendNotification("SYSTEM", "TRANSACTION_FAILED", message);
    }

    public void sendSystemAlert(String alertType, String message, Double metricValue, Double threshold) {
        String alertMessage = String.format(
                "🚨 Cảnh báo hệ thống\nLoại: %s\nMô tả: %s\nGiá trị: %s\nNgưỡng: %s",
                alertType, message, metricValue, threshold
        );

        saveAndSendNotification("SYSTEM", "SYSTEM_ALERT", alertMessage);
    }

    public void sendBatchProcessingNotification(String batchId, int total, int success, int failed, String totalAmount) {
        String successRate = total > 0 ? String.format("%.2f%%", (success * 100.0 / total)) : "0%";
        String message = String.format(
                "📊 Xử lý lô hoàn tất\nMã lô: %s\nTổng giao dịch: %d\nThành công: %d\nThất bại: %d\nTỷ lệ: %s\nTổng tiền: %s",
                batchId, total, success, failed, successRate, totalAmount
        );

        saveAndSendNotification("SYSTEM", "BATCH_PROCESSED", message);
    }

    private void saveAndSendNotification(String userId, String messageType, String messageText) {
        try {
            // Save to database
            TelegramNotification notification = new TelegramNotification();
            notification.setUserId(userId);
            notification.setChatId("ADMIN"); // In real system, get from user preferences
            notification.setMessageType(TelegramMessageType.valueOf(messageType));
            notification.setMessageText(messageText);
            notification.setStatus(NotificationStatus.valueOf("PENDING"));

            telegramNotificationRepository.save(notification);

            // Send via Telegram
            telegramService.sendMessage("ADMIN", messageText); // In real system, use actual chat ID

            // Update status
            notification.setStatus(NotificationStatus.valueOf("SENT"));
            notification.setSentAt(LocalDateTime.now());
            telegramNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }

    public List<TelegramNotification> getUserNotifications(String userId, int page, int size) {
        // Implementation for getting user notifications
        return telegramNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markNotificationAsRead(String notificationId, String userId) {
        TelegramNotification notification = telegramNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("User does not own this notification");
        }

        // Update read status if needed
        // This would require adding a read status field to the entity
    }
}
