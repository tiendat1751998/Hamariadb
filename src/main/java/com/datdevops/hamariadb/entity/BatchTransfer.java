package com.datdevops.hamariadb.entity;



import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "batch_transfers")
public class BatchTransfer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String batchId;

    @Column(length = 255)
    private String fileName;

    @Column(length = 10)
    private String fileType;

    @Column(nullable = false)
    private Integer totalTransactions = 0;

    @Column(nullable = false)
    private Integer successfulTransactions = 0;

    @Column(nullable = false)
    private Integer failedTransactions = 0;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchTransferStatus status = BatchTransferStatus.UPLOADED;

    @Column(columnDefinition = "JSON")
    private String validationErrors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "batchTransfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BatchTransferItem> batchTransferItems = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

