package com.datdevops.hamariadb.dto.request;

import lombok.Data;

import java.math.BigDecimal;

// DTO classes for batch transfer
@Data
public class BatchTransferItemRequest {
    private Integer sequenceNumber;
    private String toAccountNumber;
    private String toBankCode;
    private String toAccountName;
    private BigDecimal amount;
    private String description;

    // Getters and setters
}
