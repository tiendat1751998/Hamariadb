package com.datdevops.hamariadb.repository.dao;



import com.datdevops.hamariadb.entity.TelegramNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelegramNotificationRepository extends JpaRepository<TelegramNotification, String> {

    List<TelegramNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT tn FROM TelegramNotification tn WHERE tn.userId = :userId ORDER BY tn.createdAt DESC")
    Page<TelegramNotification> findByUserIdWithPagination(@Param("userId") String userId, Pageable pageable);

    List<TelegramNotification> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT tn FROM TelegramNotification tn WHERE tn.createdAt >= :startDate AND tn.createdAt <= :endDate")
    List<TelegramNotification> findByDateRange(@Param("startDate") java.time.LocalDateTime startDate,
                                               @Param("endDate") java.time.LocalDateTime endDate);

    long countByUserIdAndStatus(String userId, String status);
}
