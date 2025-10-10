package com.datdevops.hamariadb.dto.request;


import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScheduledTransferRequest {
    @NotBlank(message = "From account is required")
    private String fromAccount;

    @NotBlank(message = "To account is required")
    private String toAccount;

    private String toBankCode;
    private String toAccountName;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String description;

    @NotNull(message = "Execution time is required")
    private LocalDateTime executeAt;
}
