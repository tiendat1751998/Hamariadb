package com.datdevops.hamariadb.repository.mapper;


import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.datdevops.hamariadb.dto.response.AccountBalanceResponse;
import com.datdevops.hamariadb.dto.response.BatchTransferResponse;
import com.datdevops.hamariadb.dto.response.RecurringTransferResponse;
import com.datdevops.hamariadb.dto.response.ScheduledTransferResponse;
import com.datdevops.hamariadb.dto.response.TransactionHistoryResponse;
import com.datdevops.hamariadb.dto.response.TransferResponse;
import com.datdevops.hamariadb.dto.response.UserResponse;
import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.BatchTransfer;
import com.datdevops.hamariadb.entity.RecurringTransfer;
import com.datdevops.hamariadb.entity.ScheduledTransfer;
import com.datdevops.hamariadb.entity.Transaction;
import com.datdevops.hamariadb.entity.Transfer;
import com.datdevops.hamariadb.entity.User;

@Component
public class EntityMapper {

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(user.getUserRoles().stream()
                        .map(userRole -> userRole.getRole().getRoleCode())
                        .collect(Collectors.toList()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .account(user.getAccounts().stream()
                        .findFirst()
                        .map(Account::getAccountNumber)
                        .orElse(null))
                .build();
    }

    public static AccountBalanceResponse toAccountBalanceResponse(Account account) {
        return AccountBalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .currency(account.getCurrency())
                .lastUpdated(account.getUpdatedAt())
                .build();
    }

    public static TransactionHistoryResponse toTransactionHistoryResponse(Transaction transaction) {
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

    public static TransferResponse toTransferResponse(Transfer transfer) {
        return TransferResponse.builder()
                .transferId(transfer.getId())
                .transferReference(transfer.getTransferReference())
                .fromAccount(transfer.getFromAccount().getAccountNumber())
                .toAccount(transfer.getToAccountNumber())
                .amount(transfer.getAmount())
                .fee(transfer.getFee())
                .description(transfer.getDescription())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }

    public static ScheduledTransferResponse toScheduledTransferResponse(ScheduledTransfer scheduledTransfer) {
        return ScheduledTransferResponse.builder()
                .scheduleId(scheduledTransfer.getScheduleId())
                .fromAccount(scheduledTransfer.getFromAccount().getAccountNumber())
                .toAccount(scheduledTransfer.getToAccountNumber())
                .toBankCode(scheduledTransfer.getToBankCode())
                .amount(scheduledTransfer.getAmount())
                .description(scheduledTransfer.getDescription())
                .executeAt(scheduledTransfer.getExecuteAt())
                .status(scheduledTransfer.getStatus())
                .createdAt(scheduledTransfer.getCreatedAt())
                .build();
    }

    public static RecurringTransferResponse toRecurringTransferResponse(RecurringTransfer recurringTransfer) {
        return RecurringTransferResponse.builder()
                .recurringId(recurringTransfer.getRecurringId())
                .fromAccount(recurringTransfer.getFromAccount().getAccountNumber())
                .toAccount(recurringTransfer.getToAccountNumber())
                .toBankCode(recurringTransfer.getToBankCode())
                .amount(recurringTransfer.getAmount())
                .description(recurringTransfer.getDescription())
                .frequency(recurringTransfer.getFrequency())
                .startDate(recurringTransfer.getStartDate())
                .endDate(recurringTransfer.getEndDate())
                .nextExecutionDate(recurringTransfer.getNextExecutionDate())
                .status(recurringTransfer.getStatus())
                .totalOccurrences(recurringTransfer.getTotalOccurrences())
                .executedOccurrences(recurringTransfer.getExecutedOccurrences())
                .createdAt(recurringTransfer.getCreatedAt())
                .build();
    }

    public BatchTransferResponse toBatchTransferResponse(BatchTransfer batchTransfer) {
        return BatchTransferResponse.builder()
                .batchId(batchTransfer.getBatchId())
                .fileName(batchTransfer.getFileName())
                .fileType(batchTransfer.getFileType())
                .totalTransactions(batchTransfer.getTotalTransactions())
                .successfulTransactions(batchTransfer.getSuccessfulTransactions())
                .failedTransactions(batchTransfer.getFailedTransactions())
                .totalAmount(batchTransfer.getTotalAmount())
                .channel(batchTransfer.getChannel())
                .status(batchTransfer.getStatus())
                .initiatedBy(batchTransfer.getInitiatedBy().getUsername())
                .approvedBy(batchTransfer.getApprovedBy() != null ? batchTransfer.getApprovedBy().getUsername() : null)
                .createdAt(batchTransfer.getCreatedAt())
                .processedAt(batchTransfer.getProcessedAt())
                .build();
    }
}
