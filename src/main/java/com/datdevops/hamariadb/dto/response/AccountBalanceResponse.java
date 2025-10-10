package com.datdevops.hamariadb.dto.response;

import com.datdevops.hamariadb.entity.AccountStatus;
import com.datdevops.hamariadb.entity.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountBalanceResponse {
    private String accountNumber;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private LocalDateTime lastUpdated;
}
