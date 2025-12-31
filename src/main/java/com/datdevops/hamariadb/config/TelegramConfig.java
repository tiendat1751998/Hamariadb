package com.datdevops.hamariadb.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Lớp cấu hình cho việc tích hợp với Telegram Bot.
 * Đọc các thông tin cần thiết từ file application.properties và tạo các bean liên quan.
 */
@Getter // Tự động tạo các getter cho các thuộc tính
@Configuration // Đánh dấu đây là một lớp cấu hình của Spring
public class TelegramConfig {

    // Token của bot, được cung cấp bởi BotFather trên Telegram.
    @Value("${app.telegram.bot-token}")
    private String botToken;

    // ID của cuộc trò chuyện (chat) với admin để gửi các thông báo quan trọng.
    @Value("${app.telegram.admin-chat-id}")
    private String adminChatId;

    // Cờ để bật/tắt tính năng gửi thông báo qua Telegram.
    @Value("${app.telegram.enabled}")
    private boolean enabled;

    /**
     * Tạo một bean {@link TelegramBotsApi}.
     * Bean này cần thiết để đăng ký và quản lý các bot.
     * @return một instance của TelegramBotsApi.
     * @throws TelegramApiException nếu có lỗi khi khởi tạo API.
     */
    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }
}
