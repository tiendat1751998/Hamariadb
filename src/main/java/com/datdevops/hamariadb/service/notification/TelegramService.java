package com.datdevops.hamariadb.service.notification;


import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.datdevops.hamariadb.config.TelegramConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Service để gửi thông báo qua Telegram Bot API.
 */
@Slf4j
@Service
public class TelegramService {

    private final TelegramConfig telegramConfig;
    private final RestTemplate restTemplate;
    private final NumberFormat numberFormat; // Dùng để định dạng số theo kiểu tiền tệ Việt Nam.

    /**
     * Constructor để inject các dependency cần thiết.
     * @param telegramConfig Cấu hình Telegram.
     */
    public TelegramService(TelegramConfig telegramConfig) {
        this.telegramConfig = telegramConfig;
        this.restTemplate = new RestTemplate();
        // Khởi tạo NumberFormat cho locale vi-VN để có định dạng số như 1.000.000
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    /**
     * Gửi một tin nhắn văn bản đơn giản đến một chat ID cụ thể.
     * @param chatId ID của cuộc trò chuyện (có thể là user ID hoặc group ID).
     * @param message Nội dung tin nhắn.
     */
    public void sendMessage(String chatId, String message) {
        // Kiểm tra xem tính năng thông báo Telegram có được bật trong cấu hình không.
        if (!telegramConfig.isEnabled()) {
            log.debug("Telegram notifications are disabled");
            return;
        }

        // Xây dựng URL của Telegram Bot API.
        String url = "https://api.telegram.org/bot" + telegramConfig.getBotToken() + "/sendMessage";

        // Chuẩn bị body của request.
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", message);
        requestBody.put("parse_mode", "HTML"); // Cho phép sử dụng các thẻ HTML đơn giản để định dạng.

        // Chuẩn bị header của request.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Gửi request POST đến API của Telegram.
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to send Telegram message: {}", response.getBody());
            } else {
                log.debug("Telegram message sent successfully");
            }
        } catch (Exception e) {
            log.error("Exception while sending Telegram message: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo cập nhật số dư sau một giao dịch thành công.
     * @param accountNumber Số tài khoản nguồn.
     * @param toAccountNumber Số tài khoản đích.
     * @param toAccountName Tên người nhận.
     * @param amount Số tiền giao dịch (là số âm nếu là chuyển đi).
     * @param fee Phí giao dịch.
     * @param balance Số dư cuối cùng.
     * @param description Nội dung giao dịch.
     */
    public void sendBalanceUpdate(String accountNumber,String toAccountNumber, String toAccountName,BigDecimal amount, BigDecimal fee  , BigDecimal balance, String description) {
        String amountStr = numberFormat.format(amount) + " VND";
        String feeStr = numberFormat.format(fee) + " VND";
        String balanceStr = numberFormat.format(balance) + " VND";

        // Chọn emoji dựa trên việc tiền vào hay ra.
        String emoji = amount.compareTo(BigDecimal.ZERO) > 0 ? "💹" : "📤";
        String message = String.format(
                "%s Tài khoản: <b>%s</b>\nTên Người nhận: <b>%s</b> \nTài khoản nhận: <b>%s</b> \nSố tiền: <b>%s</b>\nPhí: <b> -%s</b>\nSố dư: <b>%s</b>\nNội dung: %s",
                emoji, accountNumber, toAccountName, toAccountNumber, amountStr, feeStr, balanceStr, description
        );

        // Gửi tin nhắn đến chat ID của admin đã cấu hình.
        sendMessage(telegramConfig.getAdminChatId(), message);
    }

    /**
     * Gửi thông báo khi có một giao dịch bị lỗi.
     * @param accountNumber Số tài khoản thực hiện.
     * @param amount Số tiền giao dịch.
     * @param errorMessage Thông báo lỗi.
     * @param description Nội dung giao dịch.
     */
    public void sendTransactionError(String accountNumber, BigDecimal amount, String errorMessage, String description) {
        String amountStr = numberFormat.format(amount) + " VND";
        String message = String.format(
                "❌ Giao dịch thất bại\nTài khoản: <b>%s</b>\nSố tiền: <b>%s</b>\nLỗi: %s\nNội dung: %s",
                accountNumber, amountStr, errorMessage, description
        );

        sendMessage(telegramConfig.getAdminChatId(), message);
    }

    /**
     * Gửi một cảnh báo hệ thống chung.
     * @param alertType Loại cảnh báo (ví dụ: "CPU_USAGE", "DATABASE_CONNECTION").
     * @param message Mô tả chi tiết về cảnh báo.
     * @param metricValue Giá trị đo được gây ra cảnh báo.
     * @param threshold Ngưỡng bị vượt qua.
     */
    public void sendSystemAlert(String alertType, String message, BigDecimal metricValue, BigDecimal threshold) {
        String metricStr = metricValue != null ? numberFormat.format(metricValue) : "N/A";
        String thresholdStr = threshold != null ? numberFormat.format(threshold) : "N/A";

        String alertMessage = String.format(
                "🚨 <b>Hệ thống cảnh báo</b>\nLoại: %s\nMức độ: %s\nGiá trị: %s\nNgưỡng: %s\nMô tả: %s",
                alertType, "HIGH", metricStr, thresholdStr, message
        );

        sendMessage(telegramConfig.getAdminChatId(), alertMessage);
    }

    /**
     * Gửi thông báo kết quả xử lý một giao dịch theo lô.
     * @param batchId ID của lô.
     * @param total Tổng số giao dịch trong lô.
     * @param success Số giao dịch thành công.
     * @param failed Số giao dịch thất bại.
     * @param totalAmount Tổng số tiền của lô.
     */
    public void sendBatchProcessingResult(String batchId, int total, int success, int failed, BigDecimal totalAmount) {
        String amountStr = numberFormat.format(totalAmount) + " VND";
        String successRate = total > 0 ? String.format("%.2f%%", (success * 100.0 / total)) : "0%";

        String message = String.format(
                "📊 <b>Kết quả xử lý lô</b>\nMã lô: %s\nTổng giao dịch: %d\nThành công: %d\nThất bại: %d\nTỷ lệ thành công: %s\nTổng tiền: %s",
                batchId, total, success, failed, successRate, amountStr
        );

        sendMessage(telegramConfig.getAdminChatId(), message);
    }
}
