package com.datdevops.hamariadb.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "From account is required")
    private String fromAccount;

    @NotBlank(message = "To account is required")
    private String toAccount;

    private String toBankCode;
    private String toAccountName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Minimum transfer amount is 1,000 VND")
    @DecimalMax(value = "500000000", message = "Maximum transfer amount is 500,000,000 VND")
    private BigDecimal amount;

    private String description;
}
