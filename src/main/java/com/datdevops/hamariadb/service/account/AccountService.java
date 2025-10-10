package com.datdevops.hamariadb.service.account;


import com.datdevops.hamariadb.dto.response.AccountBalanceResponse;
import com.datdevops.hamariadb.dto.response.TransactionHistoryResponse;
import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.Transaction;
import com.datdevops.hamariadb.entity.User;
import com.datdevops.hamariadb.repository.dao.AccountRepository;
import com.datdevops.hamariadb.repository.dao.TransactionRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          UserRepository userRepository, EntityMapper entityMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.entityMapper = entityMapper;
    }

    public AccountBalanceResponse getAccountBalance(String accountNumber, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if user owns the account
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this account");
        }

        return AccountBalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .currency(account.getCurrency())
                .lastUpdated(account.getUpdatedAt())
                .build();
    }

    public List<AccountBalanceResponse> getUserAccounts(String username) {
        List<Account> accounts = accountRepository.findActiveAccountsByUsername(username);

        return accounts.stream()
                .map(account -> AccountBalanceResponse.builder()
                        .accountNumber(account.getAccountNumber())
                        .balance(account.getBalance())
                        .availableBalance(account.getAvailableBalance())
                        .currency(account.getCurrency())
                        .accountType(account.getAccountType())
                        .status(account.getStatus())
                        .lastUpdated(account.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public Page<TransactionHistoryResponse> getTransactionHistory(String accountNumber, String username,
                                                                  LocalDateTime fromDate, LocalDateTime toDate,
                                                                  String transactionType, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if user owns the account
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own this account");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionRepository.findByAccountAndFilters(
                accountNumber, fromDate, toDate, transactionType, pageable);

        return transactions.map(this::convertToTransactionHistoryResponse);
    }

    private TransactionHistoryResponse convertToTransactionHistoryResponse(Transaction transaction) {
        return TransactionHistoryResponse.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .transactionDate(transaction.getTransactionDate())
                .relatedAccountNumber(transaction.getRelatedAccount() != null ?
                        transaction.getRelatedAccount().getAccountNumber() : null)
                .build();
    }
}
