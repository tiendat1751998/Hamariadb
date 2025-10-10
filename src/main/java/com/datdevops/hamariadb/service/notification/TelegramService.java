package com.datdevops.hamariadb.service.notification;


import com.datdevops.hamariadb.config.TelegramConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    private final TelegramConfig telegramConfig;
    private final RestTemplate restTemplate;
    private final NumberFormat numberFormat;

    public TelegramService(TelegramConfig telegramConfig) {
        this.telegramConfig = telegramConfig;
        this.restTemplate = new RestTemplate();
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public void sendMessage(String chatId, String message) {
        if (!telegramConfig.isEnabled()) {
            log.debug("Telegram notifications are disabled");
            return;
        }

        String url = "https://api.telegram.org/bot" + telegramConfig.getBotToken() + "/sendMessage";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", message);
        requestBody.put("parse_mode", "HTML");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
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

    public void sendBalanceUpdate(String accountNumber, BigDecimal amount, BigDecimal balance, String description) {
        String amountStr = numberFormat.format(amount) + " VND";
        String balanceStr = numberFormat.format(balance) + " VND";

        String emoji = amount.compareTo(BigDecimal.ZERO) > 0 ? "💹" : "📤";
        String message = String.format(
                "%s Tài khoản <b>%s</b> %s %s\nSố dư: <b>%s</b>\nNội dung: %s",
                emoji, accountNumber, amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "-",
                amountStr, balanceStr, description
        );

        sendMessage(telegramConfig.getAdminChatId(), message);
    }

    public void sendTransactionError(String accountNumber, BigDecimal amount, String errorMessage, String description) {
        String amountStr = numberFormat.format(amount) + " VND";
        String message = String.format(
                "❌ Giao dịch thất bại\nTài khoản: <b>%s</b>\nSố tiền: <b>%s</b>\nLỗi: %s\nNội dung: %s",
                accountNumber, amountStr, errorMessage, description
        );

        sendMessage(telegramConfig.getAdminChatId(), message);
    }

    public void sendSystemAlert(String alertType, String message, BigDecimal metricValue, BigDecimal threshold) {
        String metricStr = metricValue != null ? numberFormat.format(metricValue) : "N/A";
        String thresholdStr = threshold != null ? numberFormat.format(threshold) : "N/A";

        String alertMessage = String.format(
                "🚨 <b>Hệ thống cảnh báo</b>\nLoại: %s\nMức độ: %s\nGiá trị: %s\nNgưỡng: %s\nMô tả: %s",
                alertType, "HIGH", metricStr, thresholdStr, message
        );

        sendMessage(telegramConfig.getAdminChatId(), alertMessage);
    }

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
