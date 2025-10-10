package com.datdevops.hamariadb.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String transferReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @Column(nullable = false, length = 20)
    private String toAccountNumber;

    @Column(length = 10)
    private String toBankCode;

    @Column(length = 100)
    private String toBankName;

    @Column(length = 100)
    private String toAccountName;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 3, nullable = false)
    private String currency = "VND";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType transferType = TransferType.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}




