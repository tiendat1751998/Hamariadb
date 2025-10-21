package com.datdevops.hamariadb.controller;

import com.datdevops.hamariadb.dto.response.AccountBalanceResponse;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.TransactionHistoryResponse;
import com.datdevops.hamariadb.service.account.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountBalanceResponse>> getAccountBalance(
            @PathVariable String accountNumber,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get balance for account: {} by user: {}", accountNumber, username);

        AccountBalanceResponse balance = accountService.getAccountBalance(accountNumber, username);
        return ResponseEntity.ok(ApiResponse.success(balance, "Balance retrieved successfully"));
    }

    @GetMapping("/getaccount/")
    public ResponseEntity<ApiResponse<List<AccountBalanceResponse>>> getUserAccounts(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get accounts for user: {}", username);

        List<AccountBalanceResponse> accounts = accountService.getUserAccounts(username);
        return ResponseEntity.ok(ApiResponse.success(accounts, "Accounts retrieved successfully"));
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionHistoryResponse>>> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Get transaction history for account: {} by user: {}", accountNumber, username);

        Page<TransactionHistoryResponse> transactions = accountService.getTransactionHistory(
                accountNumber, username, fromDate, toDate, transactionType, page, size);

        return ResponseEntity.ok(ApiResponse.success(transactions, "Transaction history retrieved successfully"));
    }
}
