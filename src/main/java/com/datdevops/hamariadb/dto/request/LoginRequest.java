package com.datdevops.hamariadb.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Client public key is required")
    private String clientPublicKey;
}




