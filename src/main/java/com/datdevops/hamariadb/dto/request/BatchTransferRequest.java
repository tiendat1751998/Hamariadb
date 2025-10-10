package com.datdevops.hamariadb.dto.request;

import com.datdevops.hamariadb.entity.TransferChannel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchTransferRequest {
    @NotNull(message = "Channel is required")
    private TransferChannel channel;
    private String description;
}

