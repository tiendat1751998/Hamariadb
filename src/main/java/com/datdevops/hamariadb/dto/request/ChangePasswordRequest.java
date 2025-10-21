package com.datdevops.hamariadb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotNull(message = "UserName is required")
    private String currentPassword;
    private String newPassword;
}

