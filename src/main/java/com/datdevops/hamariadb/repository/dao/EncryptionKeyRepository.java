package com.datdevops.hamariadb.repository.dao;



import com.datdevops.hamariadb.entity.EncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, String> {

    Optional<EncryptionKey> findByKeyIdentifierAndIsActive(String keyIdentifier, Boolean isActive);

    @Query("SELECT ek FROM EncryptionKey ek WHERE ek.user.username = :username AND ek.isActive = true")
    Optional<EncryptionKey> findActiveByUsername(@Param("username") String username);

    @Modifying
    @Query("UPDATE EncryptionKey ek SET ek.isActive = false WHERE ek.user.username = :username AND ek.isActive = true")
    void deactivateUserKeys(@Param("username") String username);

    @Modifying
    @Query("UPDATE EncryptionKey ek SET ek.isActive = false WHERE ek.expiresAt < CURRENT_TIMESTAMP AND ek.isActive = true")
    void deactivateExpiredKeys();
}
