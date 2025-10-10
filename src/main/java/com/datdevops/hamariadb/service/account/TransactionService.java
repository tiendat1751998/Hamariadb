package com.datdevops.hamariadb.service.account;


import com.datdevops.hamariadb.entity.Account;
import com.datdevops.hamariadb.entity.Transaction;
import com.datdevops.hamariadb.entity.TransactionStatus;
import com.datdevops.hamariadb.entity.TransactionType;
import com.datdevops.hamariadb.repository.dao.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(Account account, Account relatedAccount,
                                         TransactionType type, BigDecimal amount,
                                         String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setAccount(account);
        transaction.setRelatedAccount(relatedAccount);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(account.getBalance());
        transaction.setBalanceAfter(account.getBalance().add(amount));
        transaction.setCurrency("VND");
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public Transaction createPendingTransaction(Account account, Account relatedAccount,
                                                TransactionType type, BigDecimal amount,
                                                String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setAccount(account);
        transaction.setRelatedAccount(relatedAccount);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(account.getBalance());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setCurrency("VND");
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public void updateTransactionToCompleted(Transaction transaction, Account account) {
        transaction.setBalanceAfter(account.getBalance());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
    }

    public void updateTransactionToFailed(Transaction transaction, String errorMessage) {
        transaction.setStatus(TransactionStatus.FAILED);
        // You might want to add an error message field to Transaction entity
        transactionRepository.save(transaction);
    }

    private String generateTransactionReference() {
        return "TX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
