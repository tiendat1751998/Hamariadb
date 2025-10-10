package com.datdevops.hamariadb.repository.dao;


import com.datdevops.hamariadb.entity.RecurringTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransferRepository extends JpaRepository<RecurringTransfer, String> {
    Optional<RecurringTransfer> findByRecurringId(String recurringId);

    @Query("SELECT r FROM RecurringTransfer r WHERE r.createdBy.username = :username ORDER BY r.nextExecutionDate ASC")
    List<RecurringTransfer> findByUsername(@Param("username") String username);

    @Query("SELECT r FROM RecurringTransfer r WHERE r.status = 'ACTIVE' AND r.nextExecutionDate <= :date")
    List<RecurringTransfer> findDueRecurringTransfers(@Param("date") LocalDate date);
}
