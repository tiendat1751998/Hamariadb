package com.datdevops.hamariadb.dto.request;


import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import com.datdevops.hamariadb.entity.Account;

@Data
public class UserCreateRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String phoneNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private List<String> roles;

    @NotBlank(message = "account name is required")
    private String account;
}
