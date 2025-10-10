package com.datdevops.hamariadb.dto.response;


import com.datdevops.hamariadb.entity.BatchTransferStatus;
import com.datdevops.hamariadb.entity.TransferChannel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BatchTransferResponse {
    private String batchId;
    private String fileName;
    private String fileType;
    private Integer totalTransactions;
    private Integer successfulTransactions;
    private Integer failedTransactions;
    private BigDecimal totalAmount;
    private TransferChannel channel;      // Changed from enum to String
    private BatchTransferStatus status;// Changed from enum to String
    private String message;
    private String initiatedBy;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
