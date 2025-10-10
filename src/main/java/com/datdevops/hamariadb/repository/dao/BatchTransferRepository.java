package com.datdevops.hamariadb.repository.dao;


import com.datdevops.hamariadb.entity.BatchTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchTransferRepository extends JpaRepository<BatchTransfer, String> {
    Optional<BatchTransfer> findByBatchId(String batchId);

    @Query("SELECT b FROM BatchTransfer b WHERE b.initiatedBy.username = :username ORDER BY b.createdAt DESC")
    java.util.List<BatchTransfer> findByUsername(@Param("username") String username);
}
