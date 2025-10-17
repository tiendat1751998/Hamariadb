package com.datdevops.hamariadb.repository.dao;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datdevops.hamariadb.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);
  
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.username = :username")
    void resetFailedLoginAttempts(@Param("username") String username);
}
