package com.datdevops.hamariadb.repository.dao;


import com.datdevops.hamariadb.entity.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, String> {
    Optional<ScheduledTransfer> findByScheduleId(String scheduleId);

    @Query("SELECT s FROM ScheduledTransfer s WHERE s.createdBy.username = :username ORDER BY s.executeAt ASC")
    List<ScheduledTransfer> findByUsername(@Param("username") String username);

    @Query("SELECT s FROM ScheduledTransfer s WHERE s.status = 'SCHEDULED' AND s.executeAt <= :now")
    List<ScheduledTransfer> findDueScheduledTransfers(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM ScheduledTransfer s WHERE s.fromAccount.accountNumber = :accountNumber AND s.status = 'SCHEDULED'")
    List<ScheduledTransfer> findScheduledByAccount(@Param("accountNumber") String accountNumber);
}
