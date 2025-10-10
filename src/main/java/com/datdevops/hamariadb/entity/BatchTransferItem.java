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
@Table(name = "batch_transfer_items")
public class BatchTransferItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_transfer_id", nullable = false)
    private BatchTransfer batchTransfer;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, length = 10)
    private String toBankCode;

    @Column(length = 100)
    private String toAccountName;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    private LocalDateTime processedAt;
}
