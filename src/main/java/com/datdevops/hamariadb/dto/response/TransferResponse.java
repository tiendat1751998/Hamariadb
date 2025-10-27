package com.datdevops.hamariadb.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.datdevops.hamariadb.entity.TransferStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferResponse {
    private String transferId;
    private String transferReference;
    private String fromAccount;  // This should be account number, not Account object
    private String toAccount;
    private BigDecimal amount;
    private BigDecimal fee;
    private String description;
    private TransferStatus status;
    // private  String message;// Changed from enum to String
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

}
