package com.datdevops.hamariadb.dto.response;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BalanceResponse {
    private String accountNumber;
    private String accountType;
    private String status;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private LocalDateTime lastUpdated;
}
