package com.datdevops.hamariadb.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "encryption_keys")
public class EncryptionKey {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true, nullable = false, length = 100,name = "key_identifier")
    private String keyIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column( name = "aes_key_encrypted",nullable = false, columnDefinition = "TEXT")
    private String aesKeyEncrypted;

    @Column(name = "iv_encrypted",nullable = false, columnDefinition = "TEXT")
    private String ivEncrypted;

    @Column(nullable = false, columnDefinition = "TEXT", name = "client_public_key")
    private String clientPublicKey;

    @Column(name = "is_active",nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at",nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
