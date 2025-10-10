package com.datdevops.hamariadb.dto.response;


import com.datdevops.hamariadb.entity.ScheduledTransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ScheduledTransferResponse {
    private String scheduleId;
    private String fromAccount;
    private String toAccount;
    private String toBankCode;
    private BigDecimal amount;
    private String description;
    private LocalDateTime executeAt;
    private ScheduledTransferStatus status;
    private LocalDateTime createdAt;
}
