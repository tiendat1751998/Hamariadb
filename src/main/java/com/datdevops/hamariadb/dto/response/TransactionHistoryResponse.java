package com.datdevops.hamariadb.dto.response;


import com.datdevops.hamariadb.entity.TransactionStatus;
import com.datdevops.hamariadb.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionHistoryResponse {
    private String transactionId;
    private String transactionReference;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private TransactionType transactionType;
    private TransactionStatus status;
    private LocalDateTime transactionDate;
    private String relatedAccountNumber;
}
