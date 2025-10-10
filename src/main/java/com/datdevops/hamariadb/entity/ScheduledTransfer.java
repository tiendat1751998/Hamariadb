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
@Table(name = "scheduled_transfers")
public class ScheduledTransfer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @Column(nullable = false, length = 20)
    private String toAccountNumber;

    @Column(length = 10)
    private String toBankCode;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime executeAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledTransferStatus status = ScheduledTransferStatus.SCHEDULED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private LocalDateTime executedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

