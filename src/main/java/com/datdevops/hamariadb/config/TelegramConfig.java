package com.datdevops.hamariadb.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Getter
@Configuration
public class TelegramConfig {

    @Value("${app.telegram.bot-token}")
    private String botToken;

    @Value("${app.telegram.admin-chat-id}")
    private String adminChatId;

    @Value("${app.telegram.enabled}")
    private boolean enabled;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }
}
