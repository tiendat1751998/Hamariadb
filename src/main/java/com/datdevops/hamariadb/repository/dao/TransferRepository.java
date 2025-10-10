package com.datdevops.hamariadb.repository.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datdevops.hamariadb.entity.Transfer;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, String> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.fromAccount.id = :accountId " +
            "AND DATE(t.createdAt) = :date AND t.status = 'COMPLETED'")
    BigDecimal sumDailyTransfersByAccount(
            @Param("accountId") String accountId,
            @Param("date") LocalDate date);

    @Query("SELECT t FROM Transfer t WHERE t.initiatedBy.username = :username ORDER BY t.createdAt DESC")
    List<Transfer> findByUsername(@Param("username") String username);

   
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.fromAccount.id = :accountId AND t.createdAt >= CURRENT_DATE AND t.status = com.datdevops.hamariadb.entity.TransferStatus.COMPLETED")
    BigDecimal sumTodayTransfersByAccount(@Param("accountId") String accountId);

}
