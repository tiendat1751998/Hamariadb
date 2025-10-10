package com.datdevops.hamariadb.repository.dao;


import com.datdevops.hamariadb.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber " +
            "AND (:fromDate IS NULL OR t.transactionDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.transactionDate <= :toDate) " +
            "AND (:type IS NULL OR t.transactionType = :type) " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccountAndFilters(
            @Param("accountNumber") String accountNumber,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("type") String type,
            Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.id = :accountId " +
            "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
            "AND t.transactionType IN ('TRANSFER_OUT', 'WITHDRAWAL')")
    Double sumWithdrawalsByAccountAndDateRange(
            @Param("accountId") String accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
