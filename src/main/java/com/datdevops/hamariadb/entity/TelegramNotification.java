package com.datdevops.hamariadb.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "telegram_notifications")
public class TelegramNotification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 100)
    private String chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TelegramMessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(columnDefinition = "JSON")
    private String messageData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

