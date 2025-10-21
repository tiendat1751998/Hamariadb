package com.datdevops.hamariadb.dto.response;


import com.datdevops.hamariadb.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private String userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String fullName;
    private UserStatus status;
    private List<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String account;
}
