package com.datdevops.hamariadb.repository.dao;

import com.datdevops.hamariadb.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(String userId);

    @Query("SELECT a FROM Account a WHERE a.user.username = :username AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByUsername(@Param("username") String username);

    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithUser(@Param("accountNumber") String accountNumber);
}
