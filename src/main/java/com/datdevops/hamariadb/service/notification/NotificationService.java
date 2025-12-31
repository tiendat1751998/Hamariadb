package com.datdevops.hamariadb.service.notification;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.datdevops.hamariadb.entity.NotificationStatus;
import com.datdevops.hamariadb.entity.TelegramMessageType;
import com.datdevops.hamariadb.entity.TelegramNotification;
import com.datdevops.hamariadb.repository.dao.TelegramNotificationRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service chịu trách nhiệm quản lý và gửi các loại thông báo khác nhau.
 * Lớp này đóng vai trò trung gian, vừa lưu thông báo vào CSDL, vừa gọi dịch vụ gửi đi (ví dụ: TelegramService).
 */
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

    /**
     * Gửi thông báo về biến động số dư.
     * @param accountNumber Số tài khoản.
     * @param amount Số tiền thay đổi.
     * @param balance Số dư cuối cùng.
     * @param description Nội dung giao dịch.
     * @return Nội dung tin nhắn đã được tạo.
     */
    public String sendBalanceNotification(String accountNumber, String amount, String balance, String description) {
        String message = String.format(
                "💰 Biến động số dư\nTài khoản: %s\nSố tiền: %s\nSố dư: %s\nNội dung: %s",
                accountNumber, amount, balance, description
        );

         saveAndSendNotification("SYSTEM", "BALANCE_UPDATE", message);
         return message;
    }

    /**
     * Gửi thông báo khi giao dịch thành công.
     * @param accountNumber Số tài khoản.
     * @param amount Số tiền.
     * @param description Nội dung.
     */
    public void sendTransactionSuccessNotification(String accountNumber, String amount, String description) {
        String message = String.format(
                "✅ Giao dịch thành công\nTài khoản: %s\nSố tiền: %s\nNội dung: %s",
                accountNumber, amount, description
        );

        saveAndSendNotification("SYSTEM", "TRANSACTION_SUCCESS", message);
    }

    /**
     * Gửi thông báo khi giao dịch thất bại.
     * @param accountNumber Số tài khoản.
     * @param amount Số tiền.
     * @param error Thông báo lỗi.
     * @param description Nội dung.
     */
    public void sendTransactionFailedNotification(String accountNumber, String amount, String error, String description) {
        String message = String.format(
                "❌ Giao dịch thất bại\nTài khoản: %s\nSố tiền: %s\nLỗi: %s\nNội dung: %s",
                accountNumber, amount, error, description
        );

        saveAndSendNotification("SYSTEM", "TRANSACTION_FAILED", message);
    }

    /**
     * Gửi thông báo cảnh báo hệ thống.
     * @param alertType Loại cảnh báo.
     * @param message Mô tả cảnh báo.
     * @param metricValue Giá trị đo được.
     * @param threshold Ngưỡng.
     */
    public void sendSystemAlert(String alertType, String message, Double metricValue, Double threshold) {
        String alertMessage = String.format(
                "🚨 Cảnh báo hệ thống\nLoại: %s\nMô tả: %s\nGiá trị: %s\nNgưỡng: %s",
                alertType, message, metricValue, threshold
        );

        saveAndSendNotification("SYSTEM", "SYSTEM_ALERT", alertMessage);
    }

    /**
     * Gửi thông báo kết quả xử lý lô.
     * @param batchId ID lô.
     * @param total Tổng số giao dịch.
     * @param success Số giao dịch thành công.
     * @param failed Số giao dịch thất bại.
     * @param totalAmount Tổng số tiền.
     */
    public void sendBatchProcessingNotification(String batchId, int total, int success, int failed, String totalAmount) {
        String successRate = total > 0 ? String.format("%.2f%%", (success * 100.0 / total)) : "0%";
        String message = String.format(
                "📊 Xử lý lô hoàn tất\nMã lô: %s\nTổng giao dịch: %d\nThành công: %d\nThất bại: %d\nTỷ lệ: %s\nTổng tiền: %s",
                batchId, total, success, failed, successRate, totalAmount
        );

        saveAndSendNotification("SYSTEM", "BATCH_PROCESSED", message);
    }

    /**
     * Phương thức nội bộ để lưu thông báo vào CSDL và gửi đi.
     * @param userId ID người dùng liên quan (hoặc "SYSTEM").
     * @param messageType Loại tin nhắn.
     * @param messageText Nội dung tin nhắn.
     */
    private void saveAndSendNotification(String userId, String messageType, String messageText) {
        TelegramNotification notification = null;
        try {
            // 1. Lưu thông báo vào CSDL với trạng thái PENDING.
            notification = new TelegramNotification();
            notification.setUserId(userId);
            // Trong hệ thống thực tế, chatId sẽ được lấy từ thông tin người dùng.
            notification.setChatId("ADMIN");
            notification.setMessageType(TelegramMessageType.valueOf(messageType));
            notification.setMessageText(messageText);
            notification.setStatus(NotificationStatus.PENDING);

            telegramNotificationRepository.save(notification);

            // 2. Gửi thông báo qua dịch vụ Telegram.
            // Trong hệ thống thực tế, chatId sẽ là notification.getChatId().
            telegramService.sendMessage("ADMIN", messageText);

            // 3. Cập nhật trạng thái thông báo thành SENT sau khi gửi thành công.
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            telegramNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Failed to send and save notification: {}", e.getMessage(), e);
            // Nếu có lỗi, cập nhật trạng thái thành FAILED.
            if (notification != null && notification.getId() != null) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                telegramNotificationRepository.save(notification);
            }
        }
    }

    /**
     * Lấy danh sách thông báo của người dùng (chưa được triển khai đầy đủ).
     * @param userId ID người dùng.
     * @param page Trang.
     * @param size Kích thước trang.
     * @return Danh sách thông báo.
     */
    public List<TelegramNotification> getUserNotifications(String userId, int page, int size) {
        // TODO: Triển khai phân trang (Paging) cho kết quả trả về.
        return telegramNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Đánh dấu một thông báo là đã đọc (chưa được triển khai đầy đủ).
     * @param notificationId ID thông báo.
     * @param userId ID người dùng.
     */
    public void markNotificationAsRead(String notificationId, String userId) {
        TelegramNotification notification = telegramNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Xác thực người dùng sở hữu thông báo này.
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("User does not own this notification");
        }

        // TODO: Cần thêm trường `isRead` vào entity TelegramNotification và cập nhật ở đây.
        // notification.setRead(true);
        // telegramNotificationRepository.save(notification);
    }
}
