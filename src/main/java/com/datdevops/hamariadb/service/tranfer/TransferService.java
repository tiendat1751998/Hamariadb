package com.datdevops.hamariadb.service.tranfer;


import com.datdevops.hamariadb.dto.request.TransferRequest;
import com.datdevops.hamariadb.dto.response.TransferResponse;
import com.datdevops.hamariadb.entity.*;
import com.datdevops.hamariadb.repository.dao.AccountRepository;
import com.datdevops.hamariadb.repository.dao.TransactionRepository;
import com.datdevops.hamariadb.repository.dao.TransferRepository;
import com.datdevops.hamariadb.repository.dao.UserRepository;
import com.datdevops.hamariadb.repository.mapper.EntityMapper;
import com.datdevops.hamariadb.service.notification.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private final EntityMapper entityMapper;

    @Value("${app.transfer.max-single-transfer}")
    private BigDecimal maxSingleTransfer;

    @Value("${app.transfer.daily-limit}")
    private BigDecimal dailyLimit;

    public TransferService(TransferRepository transferRepository, AccountRepository accountRepository,
                           TransactionRepository transactionRepository, UserRepository userRepository,
                           TelegramService telegramService, EntityMapper entityMapper) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.telegramService = telegramService;
        this.entityMapper = entityMapper;
    }

    public TransferResponse createTransfer(TransferRequest request, String username) {
        // Validate accounts and balance
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccount())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user owns the account
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User does not own the source account");
        }

        // Validate transfer amount
        validateTransferAmount(request.getAmount(), fromAccount);

        // Create transfer record
        Transfer transfer = createTransferRecord(request, fromAccount, user);

        try {
            // Process the transfer
            processTransfer(transfer, fromAccount);

            // Send success notification
            telegramService.sendBalanceUpdate(
                    fromAccount.getAccountNumber(),
                    request.getAmount().negate(),
                    fromAccount.getAvailableBalance(),
                    request.getDescription()
            );

            return entityMapper.toTransferResponse(transfer);

        } catch (Exception e) {
            // Mark transfer as failed
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setErrorMessage(e.getMessage());
            transferRepository.save(transfer);

            // Send error notification
            telegramService.sendTransactionError(
                    fromAccount.getAccountNumber(),
                    request.getAmount(),
                    e.getMessage(),
                    request.getDescription()
            );

            log.error("Transfer failed: {}", e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }

    private void validateTransferAmount(BigDecimal amount, Account fromAccount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be positive");
        }

        if (amount.compareTo(maxSingleTransfer) > 0) {
            throw new RuntimeException("Transfer amount exceeds maximum limit");
        }

        if (fromAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Check daily limit (simplified - would need more complex logic for actual implementation)
        BigDecimal dailyTotal = transferRepository.sumTodayTransfersByAccount(fromAccount.getId());
        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new RuntimeException("Transfer would exceed daily limit");
        }
    }

    private Transfer createTransferRecord(TransferRequest request, Account fromAccount, User user) {
        Transfer transfer = new Transfer();
        transfer.setTransferReference(generateTransferReference());
        transfer.setFromAccount(fromAccount);
        transfer.setToAccountNumber(request.getToAccount());
        transfer.setToBankCode(request.getToBankCode());
        transfer.setToAccountName(request.getToAccountName());
        transfer.setAmount(request.getAmount());
        transfer.setFee(calculateFee(request));
        transfer.setTotalAmount(request.getAmount().add(transfer.getFee()));
        transfer.setCurrency("VND");
        transfer.setDescription(request.getDescription());
        transfer.setTransferType(determineTransferType(request));
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setInitiatedBy(user);

        return transferRepository.save(transfer);
    }

    private void processTransfer(Transfer transfer, Account fromAccount) {
        // Deduct from source account
        fromAccount.setBalance(fromAccount.getBalance().subtract(transfer.getTotalAmount()));
        fromAccount.setAvailableBalance(fromAccount.getAvailableBalance().subtract(transfer.getTotalAmount()));
        accountRepository.save(fromAccount);

        // Create transaction record
        Transaction transaction = createTransaction(transfer, fromAccount);
        transfer.setTransaction(transaction);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());
        transferRepository.save(transfer);
    }

    private Transaction createTransaction(Transfer transfer, Account fromAccount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setAccount(fromAccount);
        transaction.setTransactionType(TransactionType.TRANSFER_OUT);
        transaction.setAmount(transfer.getTotalAmount().negate());
        transaction.setBalanceBefore(fromAccount.getBalance().add(transfer.getTotalAmount()));
        transaction.setBalanceAfter(fromAccount.getBalance());
        transaction.setCurrency("VND");
        transaction.setDescription(transfer.getDescription());
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }

    private String generateTransferReference() {
        return "TF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateTransactionReference() {
        return "TX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal calculateFee(TransferRequest request) {
        // Simplified fee calculation
        return BigDecimal.valueOf(3300); // Standard fee
    }

    private TransferType determineTransferType(TransferRequest request) {
        if (request.getToBankCode() == null || request.getToBankCode().isEmpty()) {
            return TransferType.INTERNAL;
        } else {
            return TransferType.NAPAS;
        }
    }
}
