package com.datdevops.hamariadb.dto.response;



import com.datdevops.hamariadb.entity.Frequency;
import com.datdevops.hamariadb.entity.RecurringTransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RecurringTransferResponse {
    private String recurringId;
    private String fromAccount;
    private String toAccount;
    private String toBankCode;
    private BigDecimal amount;
    private String description;
    private Frequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecutionDate;
    private RecurringTransferStatus status;
    private Integer totalOccurrences;
    private Integer executedOccurrences;
    private LocalDateTime createdAt;
}
