package com.datdevops.hamariadb.repository.dao;


import com.datdevops.hamariadb.entity.SystemAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemAlertRepository extends JpaRepository<SystemAlert, String> {

    List<SystemAlert> findByIsResolvedOrderByTriggeredAtDesc(Boolean isResolved);

    List<SystemAlert> findBySeverityAndIsResolvedOrderByTriggeredAtDesc(String severity, Boolean isResolved);

    @Query("SELECT sa FROM SystemAlert sa WHERE sa.triggeredAt >= :startDate AND sa.triggeredAt <= :endDate")
    List<SystemAlert> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(sa) FROM SystemAlert sa WHERE sa.isResolved = false AND sa.severity = :severity")
    long countUnresolvedBySeverity(@Param("severity") String severity);

    List<SystemAlert> findByAlertTypeAndIsResolvedOrderByTriggeredAtDesc(String alertType, Boolean isResolved);
}
