package com.datdevops.hamariadb.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "recurring_transfers")
public class RecurringTransfer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String recurringId;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(nullable = false)
    private Integer frequencyValue = 1;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate nextExecutionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringTransferStatus status = RecurringTransferStatus.ACTIVE;

    private Integer totalOccurrences;

    @Column(nullable = false)
    private Integer executedOccurrences = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

