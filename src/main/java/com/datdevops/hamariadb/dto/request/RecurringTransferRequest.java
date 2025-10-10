package com.datdevops.hamariadb.dto.request;


import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringTransferRequest {
    @NotBlank(message = "From account is required")
    private String fromAccount;

    @NotBlank(message = "To account is required")
    private String toAccount;

    private String toBankCode;
    private String toAccountName;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String description;
    @NotNull(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer totalOccurrences;
}

enum Frequency {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}
